package com.example.persona_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePostRequest {
    /**
     * 最终确定的动态正文
     */
    private String content;

    /**
     * 最终确定的图片链接列表 (通常是 AI 生成并转存 OSS 后的地址)
     */
    private List<String> imageUrls;
}