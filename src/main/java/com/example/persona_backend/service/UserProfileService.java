package com.example.persona_backend.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.entity.UserProfile;
import com.example.persona_backend.mapper.ChatMessageMapper;
import com.example.persona_backend.mapper.UserProfileMapper;
import com.example.persona_backend.utils.ZhipuAiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProfileService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ZhipuAiUtils zhipuAiUtils;

    @Autowired
    private AiService aiService;

    /**
     * 每次聊天后调用：检查是否需要更新画像
     * 如果用户画像不存在，会进行懒加载（自动创建）
     */
    @Async // 异步执行，不阻塞聊天接口
    public void checkAndEvolveProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);

        // 1. 懒加载：初始化新用户的画像记录
        if (profile == null) {
            log.info("👤 [Profile] 初始化新用户画像 UserId: {}", userId);
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setChatCount(0);
            userProfileMapper.insert(profile);
        }

        // 2. 累加计数
        profile.setChatCount(profile.getChatCount() + 1);

        // 3. 策略：每 10 次对话触发一次更新
        if (profile.getChatCount() % 10 == 0) {
            evolveProfile(userId, profile);
        } else {
            userProfileMapper.updateById(profile);
        }
    }

    private void evolveProfile(Long userId, UserProfile profile) {
        log.info("🧬 [Evolution] 开始进化用户画像 UserId: {}", userId);

        // 1. 获取最近 50 条聊天记录作为上下文
        LambdaQueryWrapper<ChatMessage> query = new LambdaQueryWrapper<>();
        query.eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT 50");
        List<ChatMessage> history = chatMessageMapper.selectList(query);

        if (history.isEmpty()) return;

        // 拼接对话文本
        String chatContext = history.stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 2. 调用 Kimi 总结画像
        try {
            // 使用 AiService 中专门的分析方法
            JSONObject result = aiService.analyzeUserProfile(chatContext);

            String summary = "用户喜欢探索未知"; // 兜底默认值
            String tags = "探索";

            if (result != null) {
                if (result.containsKey("summary")) summary = result.getString("summary");
                if (result.containsKey("tags")) tags = result.getString("tags");
            }

            profile.setSummary(summary);
            profile.setTags(tags);

            // 3. 调用智谱生成向量 (V_self)
            // 将 summary 和 tags 拼接起来作为 Embedding 的输入
            List<Double> vector = zhipuAiUtils.generateEmbedding(summary + " " + tags);
            profile.setTargetVector(vector);
            profile.setLastUpdated(LocalDateTime.now());

            userProfileMapper.updateById(profile);
            log.info("✅ [Evolution] 用户画像更新完成: {}", summary);

        } catch (Exception e) {
            log.error("❌ [Evolution] 画像更新失败", e);
        }
    }
}