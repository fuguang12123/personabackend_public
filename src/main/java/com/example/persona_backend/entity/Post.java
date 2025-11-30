package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("posts")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * [新增] 驱使该动态生成的用户ID
     * 对应 SQL: ADD COLUMN `user_id` BIGINT NOT NULL
     */
    private Long userId;

    private Long personaId;

    private String content;

    // 数据库里存的是 String (JSON Array)
    // 例如: '["http://...", "http://..."]'
    private String imageUrls;

    private Integer likes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}