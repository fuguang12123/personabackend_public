package com.example.persona_backend.dto;

import lombok.Data;

@Data
public class ConversationDto {
    private Long personaId;
    private String name;
    private String avatarUrl;
    private String lastMessage;

    // 返回字符串格式的时间，方便前端解析 (yyyy-MM-dd HH:mm:ss)
    private String timestamp;
}