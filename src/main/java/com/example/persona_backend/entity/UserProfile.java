package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {
    @TableId
    private Long userId;

    private String summary;

    private String tags;

    // 核心字段：用户的目标向量 (V_target)
    // MyBatis Plus 会自动将 JSON 数组转为 List<Double>
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Double> targetVector;

    private Integer chatCount;

    private LocalDateTime lastUpdated;
}