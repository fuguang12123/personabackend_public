package com.example.persona_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.persona_backend.dto.PersonaRecommendationDto;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.PersonaVector;
import com.example.persona_backend.entity.UserProfile;
import com.example.persona_backend.mapper.FollowMapper;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.PersonaVectorMapper;
import com.example.persona_backend.mapper.UserProfileMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    @Autowired
    private PersonaVectorMapper personaVectorMapper;
    @Autowired
    private PersonaMapper personaMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private FollowMapper followMapper; // 使用已有的 FollowMapper

    @Value("${moonshot.api.key}")
    private String apiKey;

    @Value("${moonshot.api.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 核心推荐流程入口
     */
    public List<PersonaRecommendationDto> recommendForUser(Long userId) {
        // 1. 构建动态目标向量 (V_target)
        List<Double> targetVector = buildTargetVector(userId);

        // 2. L0 召回：基于向量相似度获取 Top 20 候选人
        List<Persona> candidates = l0VectorRecall(targetVector, userId, 20);

        if (candidates.isEmpty()) return new ArrayList<>();

        // 3. L1 精排：Kimi 推理生成理由和匹配度
        return l1CognitiveRerank(userId, candidates);
    }

    // ================== Step 1: 构建目标向量 ==================
    private List<Double> buildTargetVector(Long userId) {
        // A. 获取 V_self (用户自身画像向量)
        UserProfile profile = userProfileMapper.selectById(userId);

        // 如果用户还没有画像（新用户），使用零向量作为起点
        List<Double> vSelf = (profile != null && profile.getTargetVector() != null)
                ? profile.getTargetVector()
                : new ArrayList<>(Collections.nCopies(1024, 0.0));

        // B. 获取 V_behavior (关注列表平均向量)
        List<Long> followedIds = followMapper.selectFollowedPersonaIds(userId);
        List<Double> vBehavior = calculateCentroid(followedIds);

        // C. 融合: V_target = alpha * V_self + (1-alpha) * V_behavior
        // 如果没有关注任何人，则 100% 依赖用户自身画像；否则 60% 依赖自身，40% 依赖关注行为
        double alpha = followedIds.isEmpty() ? 1.0 : 0.6;

        // 如果既没画像也没关注（彻底冷启动），返回零向量，L0 会退化为随机或热门
        return fuseVectors(vSelf, vBehavior, alpha);
    }

    private List<Double> calculateCentroid(List<Long> pIds) {
        if (pIds == null || pIds.isEmpty()) {
            return new ArrayList<>(Collections.nCopies(1024, 0.0));
        }

        // 批量获取已关注的 Persona 向量
        List<PersonaVector> vectors = personaVectorMapper.selectBatchIds(pIds);

        if (vectors.isEmpty()) return new ArrayList<>(Collections.nCopies(1024, 0.0));

        // 计算平均值 (Centroid)
        int dim = vectors.get(0).getEmbedding().size();
        double[] sum = new double[dim];

        for (PersonaVector pv : vectors) {
            List<Double> emb = pv.getEmbedding();
            for (int i = 0; i < dim; i++) {
                sum[i] += emb.get(i);
            }
        }

        List<Double> centroid = new ArrayList<>();
        for (double s : sum) {
            centroid.add(s / vectors.size());
        }
        return centroid;
    }

    private List<Double> fuseVectors(List<Double> v1, List<Double> v2, double alpha) {
        List<Double> result = new ArrayList<>();
        int size = Math.min(v1.size(), v2.size());
        if (size == 0) return v1;

        for (int i = 0; i < size; i++) {
            result.add(v1.get(i) * alpha + v2.get(i) * (1 - alpha));
        }
        return result;
    }

    // ================== Step 2: L0 向量召回 ==================
    private List<Persona> l0VectorRecall(List<Double> targetVector, Long userId, int topK) {
        // 获取已关注 ID 列表，用于过滤
        List<Long> followedIds = followMapper.selectFollowedPersonaIds(userId);
        if (followedIds == null) followedIds = new ArrayList<>();

        // 获取所有候选向量 (生产环境应替换为 Milvus/ElasticSearch)
        List<PersonaVector> allVectors = personaVectorMapper.selectList(null);
        if (allVectors == null) return new ArrayList<>();

        final List<Long> finalFollowedIds = followedIds;

        List<Long> topIds = allVectors.stream()
                .filter(pv -> !finalFollowedIds.contains(pv.getPersonaId())) // 过滤掉已关注的
                .map(pv -> new AbstractMap.SimpleEntry<>(pv.getPersonaId(), cosineSimilarity(targetVector, pv.getEmbedding())))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 降序排列
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topIds.isEmpty()) return new ArrayList<>();
        return personaMapper.selectBatchIds(topIds);
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ================== Step 3: L1 大模型精排 ==================
    private List<PersonaRecommendationDto> l1CognitiveRerank(Long userId, List<Persona> candidates) {
        // 1. 构造 Prompt 数据
        JSONArray candidatesJson = new JSONArray();
        for (Persona p : candidates) {
            JSONObject item = new JSONObject();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("tags", p.getPersonalityTags());
            item.put("desc", p.getDescription());
            candidatesJson.add(item);
        }

        String systemPrompt = "你是一个专业的社交推荐算法助手。以下是为用户筛选的候选人列表（JSON格式）。\n" +
                "请根据候选人的性格标签和描述，分析其与用户可能产生的化学反应。\n" +
                "任务：挑选最值得推荐的 5-6 位，并生成推荐理由（理由小于100字）。\n" +
                "要求：返回纯 JSON 数组，格式如下：\n" +
                "[{\"id\": 123, \"reason\": \"理由...\", \"matchScore\": 95}]";

        String userPrompt = "候选列表：" + candidatesJson.toString();

        // 2. 调用 Kimi
        try {
            String jsonResult = callKimiForReasoning(systemPrompt, userPrompt);
            JSONArray resultArray = JSON.parseArray(jsonResult);

            // 3. 组装最终 DTO
            List<PersonaRecommendationDto> dtos = new ArrayList<>();
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject res = resultArray.getJSONObject(i);
                Long pid = res.getLong("id");

                Persona original = candidates.stream().filter(p -> p.getId().equals(pid)).findFirst().orElse(null);
                if (original != null) {
                    PersonaRecommendationDto dto = new PersonaRecommendationDto();
                    dto.setId(original.getId());
                    dto.setName(original.getName());
                    dto.setAvatarUrl(original.getAvatarUrl());
                    dto.setTags(original.getPersonalityTags() != null ? Arrays.asList(original.getPersonalityTags().split(",")) : new ArrayList<>());
                    dto.setReason(res.getString("reason"));
                    dto.setMatchScore(res.getInteger("matchScore"));
                    dtos.add(dto);
                }
            }
            return dtos;

        } catch (Exception e) {
            log.error("L1 Rerank Failed", e);
            // 降级策略：如果 AI 失败，直接返回前 3 个
            return candidates.stream().limit(3).map(p -> {
                PersonaRecommendationDto dto = new PersonaRecommendationDto();
                dto.setId(p.getId());
                dto.setName(p.getName());
                dto.setReason("你们的性格非常契合 (数据匹配)");
                dto.setMatchScore(80);
                return dto;
            }).collect(Collectors.toList());
        }
    }

    private String callKimiForReasoning(String sys, String user) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", "moonshot-v1-8k");
        body.put("messages", JSONArray.of(
                Map.of("role", "system", "content", sys),
                Map.of("role", "user", "content", user)
        ));

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Kimi API failed: " + response.code());
            String res = response.body().string();
            String content = JSON.parseObject(res).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            return content.replaceAll("```json", "").replaceAll("```", "").trim();
        }
    }
}