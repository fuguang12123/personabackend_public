package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类
 * 对应 post_comments 表
 */
@Data
@TableName("post_comments")
public class PostComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的动态ID
    private Long postId;

    // 评论者ID (真实用户)
    private Long userId;

    // 评论内容
    private String content;

    // --- 回复逻辑字段 ---

    // 根评论ID (楼层ID)。如果这是一级评论，则为 NULL
    private Long rootParentId;

    // 直接父评论ID (你回复的那条评论的ID)。如果这是一级评论，则为 NULL
    private Long parentId;

    // 被回复的人的ID (用于前端显示 "回复 @某某")
    private Long replyToUserId;

    private LocalDateTime createdAt;
}