package com.it.gateway.controller.User;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.it.gateway.model.General.ApiResponse;
import com.it.gateway.model.User.Login;
import com.it.gateway.model.User.LoginResponse;
import com.it.gateway.model.User.RefreshToken;
import com.it.gateway.utils.RequestContext;
import com.it.gateway.service.User.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody Login login) {
        String requestId = UUID.randomUUID().toString();
        log.info("Login  request initiated: {}", requestId);
        CompletableFuture<LoginResponse> response = authService.login(login, requestId);

        try {
            LoginResponse loginResponse = response.get(120, TimeUnit.SECONDS);
            return ApiResponse.success(requestId, loginResponse, "Login success");
        } catch (Exception e) {
           return ApiResponse.error(requestId, "500", e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshToken refreshToken) {
        String requestId = UUID.randomUUID().toString();
        log.info("Refresh token request initiated: {}", requestId);
        
        LoginResponse response = authService.refreshToken(refreshToken, requestId);
        return ApiResponse.success(requestId, response, "Refresh token success");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        String requestId = UUID.randomUUID().toString();
        log.info("Logging out user: {}", RequestContext.getCurrentUsername());
        authService.logout(RequestContext.getCurrentUsername(), requestId);
        return ApiResponse.success(requestId, null, "Logout success");
    }
}
