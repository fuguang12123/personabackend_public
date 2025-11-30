package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.dto.ConversationDto;
import com.example.persona_backend.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取用户的会话列表
     * 逻辑：查询该用户与所有 Persona 的聊天记录，按 persona_id 分组，取 created_at 最大的那条
     * 注意：这里使用了 MySQL 的 DATE_FORMAT 函数来匹配前端需要的格式
     */
    @Select("SELECT " +
            "p.id AS personaId, " +
            "p.name AS name, " +
            "p.avatar_url AS avatarUrl, " +
            "cm.content AS lastMessage, " +
            "DATE_FORMAT(cm.created_at, '%Y-%m-%d %T') AS timestamp " +
            "FROM chat_messages cm " +
            "JOIN persona p ON cm.persona_id = p.id " +
            "WHERE cm.user_id = #{userId} " +
            "AND cm.created_at = ( " +
            "    SELECT MAX(created_at) " +
            "    FROM chat_messages " +
            "    WHERE user_id = #{userId} AND persona_id = cm.persona_id " +
            ") " +
            "ORDER BY cm.created_at DESC")
    List<ConversationDto> getConversations(@Param("userId") Long userId);
}