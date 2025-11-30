package com.example.persona_backend;

import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;

@SpringBootTest
class ChatMessageTest {

    @Autowired
    private ChatMessageMapper mapper;

    @Test
    void testInsertAndRead() {
        System.out.println("--- 测试数据库读写 ---");
        ChatMessage msg = new ChatMessage();
        msg.setUserId(1L);
        msg.setPersonaId(999L);
        msg.setRole("user");
        msg.setContent("Hello Database");
        msg.setCreatedAt(LocalDateTime.now());

        mapper.insert(msg);
        System.out.println("插入成功 ID: " + msg.getId());

        ChatMessage result = mapper.selectById(msg.getId());
        System.out.println("查询结果: " + result.getContent());

        assert result.getContent().equals("Hello Database");
        System.out.println("--- 测试通过 ---");
    }
}
