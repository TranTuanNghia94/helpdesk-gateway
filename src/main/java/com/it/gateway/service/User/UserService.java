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
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.Redis.RedisService;
import com.it.gateway.service.Security.JwtService;

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
public class UserService {
    private final ConcurrentMap<String, CompletableFuture<LoginResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final JwtService jwtService;
    private final RedisService redisService;
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void timeoutRequest(String requestId) {
        if (pendingRequests.containsKey(requestId)) {
            pendingRequests.get(requestId).complete(null);
            pendingRequests.remove(requestId);

            log.info("Timeout request - requestId: {}", requestId);
        }
    }

    public CompletableFuture<LoginResponse> login(Login payload, String requestId) {
        CompletableFuture<LoginResponse> future = new CompletableFuture<>();
        try {
            log.info("Login request username: {}  | requestId: {}", payload.getUsername(), requestId);

            // Add the future to pending requests before sending Kafka message
            pendingRequests.put(requestId, future);

            // Build the Kafka message
            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(requestId, Operation.LOGIN, "PROCESSING", payload);

            // Send the Kafka message
            kafkaTemplate.send(Constant.USER_EVENT_REQUEST, requestId, kafkaMessage);

        } catch (Exception e) {
            log.error("Error login username: {} | requestId: {}, \nError: {}", payload.getUsername(), requestId, e.getMessage());
            
            // Remove the future from pending requests if there's an error
            pendingRequests.remove(requestId);
            future.completeExceptionally(new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId));
        }

        return future;
    }

    public void handleLoginResponse(KafkaMessage message) {
        UserInfo userInfo = objectMapper.convertValue(message.getPayload(), UserInfo.class);
        CompletableFuture<LoginResponse> future = pendingRequests.remove(message.getMessageId());

        // Check if future exists
        if (future == null) {
            log.warn("No pending request found for messageId: {}", message.getMessageId());
            return;
        }

        try {
            if (message.getStatus().equals(Constant.ResponseStatus.ERROR.getValue())) {
                log.error("Login error response requestId: {} | \nError: {}", message.getMessageId(), message.getErrorMessage());
                future.completeExceptionally(new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message.getErrorMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), message.getMessageId()));
                return;
            }

            log.info("Login response requestId: {} | userInfo: {}", message.getMessageId(), userInfo);

            // Generate the JWT token
            String accessToken = jwtService.generateAccessToken(userInfo);
            String refreshToken = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());

            // Set the refresh token in the redis
            redisService.set(RedisKey.REFRESH_TOKEN_PREFIX + userInfo.getUsername(), refreshToken, Duration.ofHours(4));
            redisService.set(RedisKey.ACCESS_TOKEN_PREFIX + userInfo.getUsername(), accessToken, Duration.ofHours(1));
            redisService.set(userInfo.getUsername(), userInfo, Duration.ofHours(1));

            // Build the login response
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setUsername(userInfo.getUsername());
            response.setTokenType("Bearer");

            log.info("Login success response requestId: {} | username: {} ", message.getMessageId(), userInfo.getUsername());

            // Complete the future
            future.complete(response);
        } catch (Exception e) {
            log.error("Error handling login response requestId: {} | \nError: {}", message.getMessageId(), e.getMessage());

            future.completeExceptionally(new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), message.getMessageId()));
        }
    }

    public void logout(String username, String requestId) {
        try {
            log.info("Logout request username: {} | requestId: {}", username, requestId);

            // Remove the refresh token from the redis
            redisService.delete(RedisKey.REFRESH_TOKEN_PREFIX + username);
            redisService.delete(RedisKey.ACCESS_TOKEN_PREFIX + username);
            redisService.delete(username);

            log.info("Logout success username: {} | requestId: {}", username, requestId);
        } catch (Exception e) {
            log.error("Error logout username: {} | requestId: {} | \nError: {}", username, requestId, e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
