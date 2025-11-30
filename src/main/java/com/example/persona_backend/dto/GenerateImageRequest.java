package com.example.persona_backend.dto;

import lombok.Data;

@Data
public class GenerateImageRequest {
    /**
     * 图片描述/提示词
     * 用户输入的文案或专门的绘图指令
     */
    private String prompt;
}