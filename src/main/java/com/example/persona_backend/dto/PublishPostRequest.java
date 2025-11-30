package com.example.persona_backend.dto;

import lombok.Data;

@Data
public class PublishPostRequest {
    /**
     * 用户指令
     * 例如: "帮我发一条去海边玩的动态"
     */
    private String instruction;
}