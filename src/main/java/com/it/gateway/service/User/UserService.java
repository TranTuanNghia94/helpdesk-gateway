package com.it.gateway.service.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.it.gateway.enums.Constant;
import com.it.gateway.enums.Operation;
import com.it.gateway.enums.RedisKey;
import com.it.gateway.exception.ApiException;
import com.it.gateway.utils.KafkaMessageBuilder;
import com.it.gateway.utils.RequestContext;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.model.User.Login;
import com.it.gateway.model.User.LoginResponse;
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.Kafka.KafkaMessageHandlerUser;
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
    private final KafkaMessageHandlerUser kafkaMessageHandlerUser;
    private final JwtService jwtService;
    private final RedisService redisService;

    public void timeoutRequest(String requestId) {
        if (pendingRequests.containsKey(requestId)) {
            pendingRequests.get(requestId).complete(null);
            pendingRequests.remove(requestId);

            log.info("Timeout request - requestId: {}", requestId);
        }
    }

    public CompletableFuture<LoginResponse> login(Login payload) {
        CompletableFuture<LoginResponse> future = new CompletableFuture<>();
        try {
            log.info("Login request username: {}  | requestId: {}", payload.getUsername(), RequestContext.getCurrentRequestId());

            // Hash the password
            payload.hashPassword();

            // Build the Kafka message
            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(Operation.LOGIN, "PROCESSING", payload);
            
            // Send the Kafka message
            kafkaMessageHandlerUser.sendMessage(kafkaMessage, Constant.USER_EVENT_REQUEST);

            // Add the future to the pending requests
            pendingRequests.put(RequestContext.getCurrentRequestId(), future);

        } catch (Exception e) {
            log.error("Error login username: {} | requestId: {}, \nError: {}", payload.getUsername(),
                    RequestContext.getCurrentRequestId(), e.getMessage());

            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), RequestContext.getCurrentRequestId());
        }

        return future;
    }

    public void handleLoginResponse(String requestId, UserInfo userInfo) {
        try {
            log.info("Login response requestId: {} | userInfo: {}", requestId, userInfo);

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

            log.info("Login success response requestId: {} | username: {} ", requestId, userInfo.getUsername());

            // Complete the future
            pendingRequests.get(requestId).complete(response);
            pendingRequests.remove(requestId);
        } catch (Exception e) {
            log.error("Error handling login response requestId: {} | username: {} | \nError: {}", requestId, userInfo.getUsername(), e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
