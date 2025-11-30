package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty; // ✅ 必须导入这个包
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("persona")
public class Persona {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;


    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String description;


    @JsonProperty("personality_tags")
    private String personalityTags;

    @JsonProperty("prompt_template")
    private String promptTemplate;

    @JsonProperty("is_public")
    private Boolean isPublic;

    private LocalDateTime createdAt;
}