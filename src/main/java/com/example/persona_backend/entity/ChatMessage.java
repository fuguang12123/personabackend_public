package com.example.persona_backend.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_messages")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long personaId;
    private String role; // "user" 或 "assistant"
    private String content;
    private LocalDateTime createdAt;

    // [New] 多模态字段
    @TableField("msg_type")
    private Integer msgType; // 0:Text, 1:Image, 2:Audio

    @TableField("media_url")
    private String mediaUrl;

    private Integer duration;

    @TableField("extra_info")
    private String extraInfo;
}