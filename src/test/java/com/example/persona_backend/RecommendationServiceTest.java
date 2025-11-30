package com.example.persona_backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.dto.PersonaRecommendationDto;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.PersonaVector;
import com.example.persona_backend.entity.UserProfile;
import com.example.persona_backend.mapper.FollowMapper;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.PersonaVectorMapper;
import com.example.persona_backend.mapper.UserProfileMapper;
import com.example.persona_backend.service.RecommendationService;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private PersonaVectorMapper personaVectorMapper;
    @Mock
    private PersonaMapper personaMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private FollowMapper followMapper;

    // Mock HTTP Client for Kimi API
    @Mock
    private OkHttpClient client;
    @Mock
    private Call call;
    @Mock
    private Response response;
    @Mock
    private ResponseBody responseBody;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        // 利用反射注入私有字段
        ReflectionTestUtils.setField(recommendationService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(recommendationService, "apiUrl", "http://test-api.com");
        ReflectionTestUtils.setField(recommendationService, "client", client);
    }

    @Test
    void testRecommend_SuccessFlow() throws IOException {
        Long userId = 1L;

        // 1. Mock Data: User Profile (用户画像)
        UserProfile mockProfile = new UserProfile();
        mockProfile.setUserId(userId);
        mockProfile.setTargetVector(Arrays.asList(0.1, 0.1));
        when(userProfileMapper.selectById(userId)).thenReturn(mockProfile);

        // 2. Mock Data: Follow List (关注列表为空)
        when(followMapper.selectFollowedPersonaIds(userId)).thenReturn(new ArrayList<>());

        // 3. Mock Data: Candidate Vectors (候选向量)
        PersonaVector pv1 = new PersonaVector(); pv1.setPersonaId(101L); pv1.setEmbedding(Arrays.asList(0.1, 0.1)); // 匹配
        PersonaVector pv2 = new PersonaVector(); pv2.setPersonaId(102L); pv2.setEmbedding(Arrays.asList(0.9, 0.9)); // 不匹配
        when(personaVectorMapper.selectList(null)).thenReturn(Arrays.asList(pv1, pv2));

        // 4. Mock Data: Persona Details (候选人详情)
        Persona p1 = new Persona(); p1.setId(101L); p1.setName("AI Expert");
        when(personaMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(p1));

        // 5. Mock Kimi API Response (L1 精排结果)
        String mockAiJson = "[{\"id\": 101, \"reason\": \"He likes AI too.\", \"matchScore\": 95}]";
        String mockApiResponse = "{\"choices\":[{\"message\":{\"content\":\"" + mockAiJson.replace("\"", "\\\"") + "\"}}]}";

        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(mockApiResponse);

        // --- Execute (执行) ---
        List<PersonaRecommendationDto> result = recommendationService.recommendForUser(userId);

        // --- Verify (验证) ---
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AI Expert", result.get(0).getName());

        // ✅ 修正点：这里验证调用了 2 次 (一次在 buildTargetVector, 一次在 l0VectorRecall)
        verify(followMapper, times(2)).selectFollowedPersonaIds(userId);
    }
}