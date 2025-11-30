package com.example.persona_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.dto.ConversationDto;
import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.UserProfile; // 引入实体
import com.example.persona_backend.mapper.ChatMessageMapper;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.UserProfileMapper; // 引入Mapper
import com.example.persona_backend.utils.AliyunOSSOperator;
import com.example.persona_backend.utils.VolcEngineUtils;
import com.example.persona_backend.utils.ZhipuAiUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils; // 引入工具类

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    @Value("${moonshot.api.key}")
    private String apiKey;

    @Value("${moonshot.api.url}")
    private String apiUrl;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private PersonaMapper personaMapper;

    @Autowired
    private VolcEngineUtils volcEngineUtils;

    @Autowired
    private ZhipuAiUtils zhipuAiUtils;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Autowired
    private UserProfileService userProfileService;

    // ✅ [修改点 1] 注入 UserProfileMapper 以便读取用户画像
    @Autowired
    private UserProfileMapper userProfileMapper;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    // ================= 1. 文本/生图聊天 =================
    public ChatMessage chat(Long userId, Long personaId, String userContent, boolean isImageGen) {
        Persona persona = validatePersona(personaId);

        // 存用户消息
        saveMessage(userId, personaId, "user", userContent, 0, null, 0);

        // 调用核心处理逻辑
        ChatMessage response = processAiInteraction(userId, persona, userContent, false, isImageGen);

        // 触发画像进化
        userProfileService.checkAndEvolveProfile(userId);

        return response;
    }

    // ================= 2. 语音聊天 =================
    public ChatMessage chatWithAudio(Long userId, Long personaId, MultipartFile audioFile, Integer duration) throws Exception {
        Persona persona = validatePersona(personaId);
        String originalFilename = audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio.wav";
        String userAudioUrl = aliyunOSSOperator.upload(audioFile.getBytes(), originalFilename);

        String recognizedText = volcEngineUtils.recognizeAudio(audioFile.getBytes(), "wav");

        saveMessage(userId, personaId, "user", recognizedText, 2, userAudioUrl, duration);

        // 调用核心处理逻辑
        ChatMessage response = processAiInteraction(userId, persona, recognizedText, true, false);

        userProfileService.checkAndEvolveProfile(userId);

        return response;
    }

    // ================= 核心处理逻辑 =================

    private ChatMessage processAiInteraction(Long userId, Persona persona, String userText, boolean replyVoice, boolean isImageGen) {

        // 1. 构建 Prompt (这里会读取用户画像)
        List<Map<String, String>> messages = buildPromptContext(userId, persona, userText, isImageGen);

        // 2. 调用 LLM
        String aiRawReply = callKimiApi(messages);
        log.info("🤖 AI 原始回复 (IsImageGen={}): {}", isImageGen, aiRawReply);

        // 3. 处理回复
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setUserId(userId);
        aiMsg.setPersonaId(persona.getId());
        aiMsg.setRole("assistant");
        aiMsg.setCreatedAt(java.time.LocalDateTime.now());

        JSONObject command = tryParseJsonCommand(aiRawReply);

        if (command != null && "DRAW".equalsIgnoreCase(command.getString("action"))) {
            // === 分支 A: 生图 ===
            String imagePrompt = command.getString("prompt");
            try {
                String imageUrl = zhipuAiUtils.generateImage(imagePrompt);
                aiMsg.setMsgType(1); // Image
                aiMsg.setContent(imagePrompt);
                aiMsg.setMediaUrl(imageUrl);
                aiMsg.setDuration(0);
            } catch (Exception e) {
                log.error("生图失败", e);
                aiMsg.setMsgType(0);
                aiMsg.setContent("（图片生成失败: " + e.getMessage() + "）");
            }
        } else {
            // === 分支 B: 对话 ===
            aiMsg.setContent(aiRawReply);

            if (replyVoice) {
                aiMsg.setMsgType(2);
                try {
                    byte[] ttsBytes = volcEngineUtils.synthesizeSpeech(aiRawReply, "neutral");
                    if (ttsBytes != null && ttsBytes.length > 0) {
                        String ttsUrl = aliyunOSSOperator.upload(ttsBytes, "tts_" + UUID.randomUUID() + ".mp3");
                        aiMsg.setMediaUrl(ttsUrl);
                        aiMsg.setDuration(Math.max(1, aiRawReply.length() / 4));
                    } else {
                        aiMsg.setMsgType(0);
                    }
                } catch (Exception e) {
                    aiMsg.setMsgType(0);
                }
            } else {
                aiMsg.setMsgType(0); // Text
            }
        }

        chatMessageMapper.insert(aiMsg);
        return aiMsg;
    }

    private List<Map<String, String>> buildPromptContext(Long userId, Persona persona, String userContent, boolean isImageGen) {
        List<Map<String, String>> messages = new ArrayList<>();

        // ✅ [修改点 2] 获取用户画像
        UserProfile userProfile = userProfileMapper.selectById(userId);

        // System Prompt (传入 userProfile)
        String systemContent = buildEnrichedSystemPrompt(persona, userProfile);
        messages.add(Map.of("role", "system", "content", systemContent));

        // History
        LambdaQueryWrapper<ChatMessage> query = new LambdaQueryWrapper<>();
        query.eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getPersonaId, persona.getId())
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT 20");

        List<ChatMessage> history = chatMessageMapper.selectList(query)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .collect(Collectors.toList());

        for (ChatMessage msg : history) {
            String content = msg.getContent();
            if (msg.getMsgType() == 1) content = "[发送了一张图片: " + content + "]";
            messages.add(Map.of("role", msg.getRole(), "content", content));
        }

        // Add Current Message
        String finalUserContent = userContent;
        if (isImageGen) {
            finalUserContent = userContent + "\n\n(系统指令：用户明确要求根据上述内容生成一张图片。请忽略对话逻辑，**必须**直接返回 JSON 格式的 DRAW 指令，prompt 字段需根据角色人设进行丰富的画面联想和英文翻译。)";
        }

        boolean inHistory = !history.isEmpty() && history.get(history.size() - 1).getContent().equals(userContent);
        if (!inHistory || isImageGen) {
            messages.add(Map.of("role", "user", "content", finalUserContent));
        }

        return messages;
    }

    private Persona validatePersona(Long personaId) {
        Persona persona = personaMapper.selectById(personaId);
        if (persona == null) throw new RuntimeException("Persona not found");
        return persona;
    }

    private void saveMessage(Long userId, Long personaId, String role, String content, int msgType, String mediaUrl, int duration) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setPersonaId(personaId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setMediaUrl(mediaUrl);
        msg.setDuration(duration);
        msg.setCreatedAt(java.time.LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

    // ✅ [修改点 3] 修改方法签名，接收 UserProfile，并根据画像是否存在来注入 Prompt
    private String buildEnrichedSystemPrompt(Persona persona, UserProfile userProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("你现在需要完全沉浸地扮演以下角色与用户对话，不要暴露你是AI模型：\n");
        sb.append("【角色档案】\n");
        sb.append("名字：").append(persona.getName()).append("\n");
        if (persona.getDescription() != null) sb.append("简介：").append(persona.getDescription()).append("\n");
        if (persona.getPersonalityTags() != null) sb.append("性格标签：").append(persona.getPersonalityTags()).append("\n");

        // === 新增：注入用户画像逻辑 ===
        if (userProfile != null && (StringUtils.hasText(userProfile.getSummary()) || StringUtils.hasText(userProfile.getTags()))) {
            sb.append("\n【当前对话用户画像】(重要：请根据此信息调整你的语气和话题)\n");

            if (StringUtils.hasText(userProfile.getSummary())) {
                sb.append("用户性格/状态：").append(userProfile.getSummary()).append("\n");
            }

            if (StringUtils.hasText(userProfile.getTags())) {
                sb.append("用户兴趣标签：").append(userProfile.getTags()).append("\n");
            }

            sb.append("指令：请根据用户的性格和兴趣，让回复更具共情力，主动聊用户感兴趣的话题，避开用户反感的方式。\n");
        }
        // ============================

        sb.append("\n【重要：多模态响应协议】\n");
        sb.append("1. 用户可能会用语音与你交流，请用口语化、自然的语气回复。\n");
        sb.append("2. 【意图识别】：如果用户明确要求'画图'、'看照片'、'发自拍'，或者系统指令提示要求生图，请**不要**回复普通文本，而是严格返回以下 JSON 格式：\n");
        sb.append("{\"action\": \"DRAW\", \"prompt\": \"<基于人设优化的英文生图提示词>\"}\n");
        sb.append("3. 如果是普通对话，直接输出文本即可，不要包含 JSON。\n");

        sb.append("\n【核心扮演指令】\n");
        sb.append(persona.getPromptTemplate() != null ? persona.getPromptTemplate() : "请自由发挥。");
        return sb.toString();
    }

    private JSONObject tryParseJsonCommand(String text) {
        try {
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
                if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
                if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            }
            return JSON.parseObject(text.trim());
        } catch (Exception e) { return null; }
    }

    private String callKimiApi(List<Map<String, String>> messages) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "moonshot-v1-32k");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.8);

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("API Error: " + response.code());
            return JSON.parseObject(response.body().string()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            log.error("AI Error", e);
            return "(AI 思考超时)";
        }
    }

    public List<ChatMessage> getHistory(Long userId, Long personaId) {
        LambdaQueryWrapper<ChatMessage> query = new LambdaQueryWrapper<>();
        query.eq(ChatMessage::getUserId, userId).eq(ChatMessage::getPersonaId, personaId).orderByAsc(ChatMessage::getCreatedAt);
        return chatMessageMapper.selectList(query);
    }
    public List<ConversationDto> getConversationList(Long userId) { return chatMessageMapper.getConversations(userId); }
}