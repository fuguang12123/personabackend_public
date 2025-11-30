package com.example.persona_backend.dto;

import lombok.Data;

@Data
public class MagicEditRequest {
    /**
     * 需要润色的原始内容
     */
    private String content;

    /**
     * 智能体名字
     */
    private String personaName;

    /**
     * [New] 智能体背景描述 (用于增强 AI 理解)
     */
    private String description;

    /**
     * [New] 智能体性格标签 (用于增强 AI 理解)
     */
    private String tags;
}