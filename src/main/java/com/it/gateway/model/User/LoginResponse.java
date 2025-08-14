package com.it.gateway.model.User;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String tokenType;
}
