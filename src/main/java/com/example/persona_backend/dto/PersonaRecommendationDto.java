package com.example.persona_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class PersonaRecommendationDto {
    private Long id;              // Persona ID
    private String name;          // 名称
    private String avatarUrl;     // 头像
    private List<String> tags;    // 标签列表
    private String reason;        // 推荐理由 (由 Kimi 生成)
    private Integer matchScore;   // 匹配度 (0-100)
}