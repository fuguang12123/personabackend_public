package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体类
 * 对应 notifications 表
 */
@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 接收通知的用户ID
    private Long receiverId;

    // 触发通知的用户ID (谁给你点的赞)
    private Long senderId;

    /**
     * 通知类型:
     * 1 = 点赞动态
     * 2 = 评论动态
     * 3 = 回复评论
     */
    private Integer type;

    // 关联的目标ID (通常是 PostID，点击通知跳转到哪里)
    private Long targetId;

    // 是否已读 (0=未读, 1=已读)
    private Boolean isRead;

    private LocalDateTime createdAt;
}