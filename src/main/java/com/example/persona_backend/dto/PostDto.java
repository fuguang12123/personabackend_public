package com.example.persona_backend.dto;

import lombok.Data;
import java.util.List;

/**
 * 发送给客户端的扁平化数据对象
 * ✅ [Update] 新增 isBookmarked 和 userId 以支持前端完整展示
 */
@Data
public class PostDto {
    private Long id;
    private String personaId; // String 方便前端处理
    private String content;

    private List<String> imageUrls;

    private Integer likes;
    private Long createdAt;

    // --- 快照字段 ---
    private String authorName;
    private String authorAvatar;

    // --- 交互状态 ---
    private Boolean isLiked;      // 是否点赞

    // 🔥 [新增] 是否收藏 (用于动态列表显示星星)
    private Boolean isBookmarked;

    // 🔥 [新增] 发帖人ID (用于点击跳转或权限判断)
    private Long userId;
}