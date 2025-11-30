package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    // [New] 个人信息字段
    private String avatarUrl;
    private String backgroundImageUrl;
    private String nickname; // 昵称（可选）

    private LocalDateTime createdAt;
}