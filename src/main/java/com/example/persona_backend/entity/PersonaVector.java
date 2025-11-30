package com.example.persona_backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "persona_vector", autoResultMap = true)
public class PersonaVector {
    @TableId
    private Long personaId;

    // 核心字段：Persona 的特征向量 (V_persona)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Double> embedding;

    private Integer version;
}