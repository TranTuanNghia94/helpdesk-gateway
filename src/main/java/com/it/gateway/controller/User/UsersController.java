package com.it.gateway.controller.User;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.it.gateway.model.General.ApiResponse;
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.User.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfo> getMe() {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting user info request initiated: {}", requestId);

        UserInfo userInfo = userService.getUserInfo(requestId);
        return ApiResponse.success(requestId, userInfo, "Get me success");
    }
}
