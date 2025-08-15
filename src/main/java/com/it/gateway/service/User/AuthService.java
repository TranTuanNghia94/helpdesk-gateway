package com.it.gateway.service.User;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.enums.Constant;
import com.it.gateway.enums.Operation;
import com.it.gateway.enums.RedisKey;
import com.it.gateway.exception.ApiException;
import com.it.gateway.utils.KafkaMessageBuilder;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.model.User.Login;
import com.it.gateway.model.User.LoginResponse;
import com.it.gateway.model.User.RefreshToken;
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.Redis.RedisService;
import com.it.gateway.service.Security.JwtService;
import com.it.gateway.utils.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final ConcurrentMap<String, CompletableFuture<LoginResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final JwtService jwtService;
    private final RedisService redisService;
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofHours(4);
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(1);
    private static final Duration USER_INFO_DURATION = Duration.ofHours(4);

    public void timeoutRequest(String requestId) {
        CompletableFuture<LoginResponse> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(null);
            log.info("Timeout request - requestId: {}", requestId);
        }
    }

    public CompletableFuture<LoginResponse> login(Login payload, String requestId) {
        CompletableFuture<LoginResponse> future = new CompletableFuture<>();
        try {
            log.info("Login request username: {} | requestId: {}", payload.getUsername(), requestId);
            pendingRequests.put(requestId, future);

            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(requestId, Operation.LOGIN, "PROCESSING", payload);
            kafkaTemplate.send(Constant.USER_EVENT_REQUEST, requestId, kafkaMessage);

        } catch (Exception e) {
            log.error("Error login username: {} | requestId: {} | Error: {}", payload.getUsername(), requestId, e.getMessage());
            pendingRequests.remove(requestId);
            future.completeExceptionally(createApiException(e.getMessage(), requestId));
        }
        return future;
    }

    public void handleLoginResponse(KafkaMessage message) {
        CompletableFuture<LoginResponse> future = pendingRequests.remove(message.getMessageId());
        if (future == null) {
            log.warn("No pending request found for messageId: {}", message.getMessageId());
            return;
        }

        try {
            if (Constant.ResponseStatus.ERROR.getValue().equals(message.getStatus())) {
                log.error("Login error response requestId: {} | Error: {}", message.getMessageId(), message.getErrorMessage());
                future.completeExceptionally(createApiException(message.getErrorMessage(), message.getMessageId()));
                return;
            }

            UserInfo userInfo = objectMapper.convertValue(message.getPayload(), UserInfo.class);
            log.info("Login response requestId: {} | userInfo: {}", message.getMessageId(), userInfo);

            LoginResponse response = createLoginResponse(userInfo);
            storeUserTokens(userInfo, response.getAccessToken(), response.getRefreshToken());

            log.info("Login success response requestId: {} | username: {}", message.getMessageId(), userInfo.getUsername());
            future.complete(response);

        } catch (Exception e) {
            log.error("Error handling login response requestId: {} | Error: {}", message.getMessageId(), e.getMessage());
            future.completeExceptionally(createApiException(e.getMessage(), message.getMessageId()));
        }
    }

    public void logout(String username, String requestId) {
        try {
            log.info("Logout request username: {} | requestId: {}", username, requestId);
            removeUserTokens(username);
            log.info("Logout success username: {} | requestId: {}", username, requestId);
        } catch (Exception e) {
            log.error("Error logout username: {} | requestId: {} | Error: {}", username, requestId, e.getMessage());
            throw createApiException(e.getMessage(), requestId);
        }
    }

    public LoginResponse refreshToken(RefreshToken refreshToken, String requestId) {
        try {
            String username = RequestContext.getCurrentUsername();
            log.info("Refresh token request username: {} | requestId: {}", username, requestId);

            if (!isValidRefreshToken(username, refreshToken.getRefreshToken())) {
                log.warn("Refresh token not found username: {} | requestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "Invalid refresh token", 
                    HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            UserInfo userInfo = getUserInfoFromRedis(username);
            LoginResponse response = createLoginResponse(userInfo);
            storeUserTokens(userInfo, response.getAccessToken(), response.getRefreshToken());

            return response;
        } catch (Exception e) {
            log.error("Error refresh token username: {} | requestId: {} | Error: {}", 
                RequestContext.getCurrentUsername(), requestId, e.getMessage());
            throw createApiException(e.getMessage(), requestId);
        }
    }

    private LoginResponse createLoginResponse(UserInfo userInfo) {
        String accessToken = jwtService.generateAccessToken(userInfo);
        String refreshToken = generateRefreshToken();
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUsername(userInfo.getUsername());
        response.setTokenType("Bearer");
        
        return response;
    }

    private void storeUserTokens(UserInfo userInfo, String accessToken, String refreshToken) {
        String username = userInfo.getUsername();
        redisService.set(RedisKey.REFRESH_TOKEN_PREFIX + username, refreshToken, REFRESH_TOKEN_DURATION);
        redisService.set(RedisKey.ACCESS_TOKEN_PREFIX + username, accessToken, ACCESS_TOKEN_DURATION);
        redisService.set(username, userInfo, USER_INFO_DURATION);
    }

    private void removeUserTokens(String username) {
        redisService.delete(RedisKey.REFRESH_TOKEN_PREFIX + username);
        redisService.delete(RedisKey.ACCESS_TOKEN_PREFIX + username);
        redisService.delete(username);
    }

    private boolean isValidRefreshToken(String username, String refreshToken) {
        Object user = redisService.get(username);
        String redisRefreshToken = (String) redisService.get(RedisKey.REFRESH_TOKEN_PREFIX + username);
        return user != null && redisRefreshToken != null && redisRefreshToken.equals(refreshToken);
    }

    private UserInfo getUserInfoFromRedis(String username) {
        Object user = redisService.get(username);
        return objectMapper.convertValue(user, UserInfo.class);
    }

    private String generateRefreshToken() {
        return Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
    }

    private ApiException createApiException(String message, String requestId) {
        return new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message,
            HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    }
}
