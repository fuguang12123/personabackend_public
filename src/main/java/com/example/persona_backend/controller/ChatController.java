package com.example.persona_backend.controller;

import com.example.persona_backend.common.Result;
import com.example.persona_backend.dto.ConversationDto;
import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 纯文本/生图请求接口
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long personaId = Long.valueOf(params.get("personaId").toString());
        String content = (String) params.get("content");

        // ✅ [New] 获取生图标记 (默认为 false)
        // 这样不影响旧的普通聊天逻辑，只有前端明确传 true 时才生效
        boolean isImageGen = params.containsKey("isImageGen") && Boolean.parseBoolean(params.get("isImageGen").toString());

        ChatMessage response = chatService.chat(userId, personaId, content, isImageGen);
        return Result.success(response);
    }

    // 语音发送接口 (保持不变)
    @PostMapping("/sendAudio")
    public Result<ChatMessage> sendAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("personaId") Long personaId,
            @RequestParam(value = "duration", defaultValue = "0") Integer duration
    ) {
        if (file.isEmpty()) return Result.error("Audio file is empty");
        try {
            return Result.success(chatService.chatWithAudio(userId, personaId, file, duration));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Audio chat failed: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam Long userId, @RequestParam Long personaId) {
        return Result.success(chatService.getHistory(userId, personaId));
    }

    @GetMapping("/conversations")
    public Result<List<ConversationDto>> getConversations(@RequestParam Long userId) {
        return Result.success(chatService.getConversationList(userId));
    }
}