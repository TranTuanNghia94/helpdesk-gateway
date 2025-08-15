package com.it.gateway.service.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.it.gateway.service.Redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.exception.ApiException;
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.utils.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public UserInfo getUserInfo(String requestId) {
        try {
            log.info("Getting user info username: {} | requestId: {}", RequestContext.getCurrentUsername(), requestId);
            String username = RequestContext.getCurrentUsername();
            Object user = redisService.get(username);
            if (user == null) {
                log.error("User not found username: {} | requestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "User not found", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
            }
            UserInfo userInfo = objectMapper.convertValue(user, UserInfo.class);
            log.info("Request ID: {} | User info found: {}", requestId, userInfo.getUsername());
            
            return userInfo;
        } catch (Exception e) {
            log.error("Error getting user info username: {} | error: {}", RequestContext.getCurrentUsername(), e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting user info", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
