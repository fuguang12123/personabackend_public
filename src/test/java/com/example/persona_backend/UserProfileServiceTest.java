package com.example.persona_backend;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.entity.ChatMessage;
import com.example.persona_backend.entity.UserProfile;
import com.example.persona_backend.mapper.ChatMessageMapper;
import com.example.persona_backend.mapper.UserProfileMapper;
import com.example.persona_backend.service.AiService;
import com.example.persona_backend.service.UserProfileService;
import com.example.persona_backend.utils.ZhipuAiUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ZhipuAiUtils zhipuAiUtils;
    @Mock
    private AiService aiService;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void testLazyInit_And_NoTrigger() {
        // --- 场景：新用户，第一次聊天 ---
        Long userId = 999L;

        // 1. Mock: 数据库查不到画像 (返回 null)
        when(userProfileMapper.selectById(userId)).thenReturn(null);

        // --- 执行 ---
        userProfileService.checkAndEvolveProfile(userId);

        // --- 验证 ---
        // 1. 验证是否执行了 insert (懒加载)
        verify(userProfileMapper).insert(any(UserProfile.class));

        // 2. 验证此时 chatCount 应该是 1，不应该触发 AI 分析
        verify(aiService, never()).analyzeUserProfile(anyString());
    }

    @Test
    void testTrigger_At_10th_Message() {
        // --- 场景：老用户，发送第 10 条消息 ---
        Long userId = 1L;

        // 1. Mock: 用户当前 chatCount = 9
        UserProfile existingProfile = new UserProfile();
        existingProfile.setUserId(userId);
        existingProfile.setChatCount(9);
        when(userProfileMapper.selectById(userId)).thenReturn(existingProfile);

        // 2. Mock: 聊天记录
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage());
        when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(history);

        // 3. Mock: AiService 分析结果
        JSONObject mockAnalysis = new JSONObject();
        mockAnalysis.put("summary", "User is curious");
        mockAnalysis.put("tags", "Sci-Fi,Coding");
        when(aiService.analyzeUserProfile(anyString())).thenReturn(mockAnalysis);

        // 4. Mock: Zhipu Embedding
        when(zhipuAiUtils.generateEmbedding(anyString())).thenReturn(Arrays.asList(0.1, 0.2, 0.3));

        // --- 执行 ---
        userProfileService.checkAndEvolveProfile(userId);

        // --- 验证 ---
        // 1. 验证计数器变为 10
        assertEquals(10, existingProfile.getChatCount());

        // 2. 验证是否调用了 AI 分析
        verify(aiService).analyzeUserProfile(anyString());

        // 3. 验证是否生成了向量
        verify(zhipuAiUtils).generateEmbedding("User is curious Sci-Fi,Coding");

        // 4. 验证是否更新了数据库
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).updateById(captor.capture());

        UserProfile updatedProfile = captor.getValue();
        assertEquals("User is curious", updatedProfile.getSummary());
        assertNotNull(updatedProfile.getTargetVector());
    }
}