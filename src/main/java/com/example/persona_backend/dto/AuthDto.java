package com.example.persona_backend.dto;

import lombok.Data;

// 把请求和响应的格式都定义在这里，方便管理
public class AuthDto {

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String username;
        private String message; // 错误或成功信息

        public AuthResponse(String token, Long userId, String username, String message) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.message = message;
        }
    }
}