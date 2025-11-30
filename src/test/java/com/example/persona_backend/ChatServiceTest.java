package com.example.persona_backend;

import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChatServiceTest {

    @Autowired private ChatService chatService;
    @Autowired private PersonaMapper personaMapper;

    @Test
    void testChatFlow() {
        System.out.println("--- 测试 AI 对话流 ---");
        // 1. 确保有一个测试用的 Persona
        Long testPersonaId = 1L;
        // 如果数据库没数据，先插一条 (可选)

        Persona p = new Persona();
        p.setName("TestBot");
        p.setPromptTemplate("你是一只猫，每句话结尾都要带'喵'");
        p.setUserId(1L);
        personaMapper.insert(p);
        testPersonaId = p.getId();

        // 2. 发送消息
        ChatMessage response = chatService.chat(1L, testPersonaId, "你好，你是谁？",false);

        System.out.println("AI 回复: " + response.getContent());

        assert response.getRole().equals("assistant");
        assert response.getContent().length() > 0;
        System.out.println("--- 测试通过 ---");
    }
}
