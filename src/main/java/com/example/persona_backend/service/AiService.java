package com.example.persona_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.example.persona_backend.utils.AliyunOSSOperator;
import com.example.persona_backend.utils.ZhipuAiUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiService {

    @Value("${moonshot.api.key}")
    private String apiKey;

    @Value("${moonshot.api.url}")
    private String apiUrl;

    @Autowired
    private ZhipuAiUtils zhipuAiUtils;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    // ================== 新增方法：用户画像分析 ==================
    /**
     * [New] 分析用户画像
     * 专门用于从聊天记录中提取用户性格和兴趣
     * @param chatContext 聊天记录拼接的字符串
     * @return JSON {summary: "...", tags: "..."}
     */
    public JSONObject analyzeUserProfile(String chatContext) {
        String systemPrompt = "你是一个专业的心理侧写师。请阅读用户的聊天记录，总结用户的【性格特征(summary)】和【兴趣标签(tags)】。\n" +
                "要求：\n" +
                "1. summary 在50字以内，侧写用户的心理状态。\n" +
                "2. tags 用逗号分隔，提取3-5个。\n" +
                "请务必只返回纯 JSON 格式，不要包含 markdown 标记，格式如下：\n" +
                "{\"summary\": \"...\", \"tags\": \"...\"}";

        // 复用底层的 callMoonshot 方法
        String jsonStr = callMoonshot(systemPrompt, chatContext, true);

        // 清理可能存在的 markdown 标记
        jsonStr = jsonStr.replaceAll("```json", "").replaceAll("```", "").trim();
        return JSON.parseObject(jsonStr);
    }

    // ================== 原有方法保持不变 ==================

    public String generatePersonaDescription(String name) {
        String systemPrompt = "你是一个专业的角色设计大师。请根据用户提供的【角色名】，进行丰富的联想和创作，生成以下三部分内容：\n" +
                "1. description: 一段引人入胜的角色背景描述和性格介绍（100字以内）。\n" +
                "2. tags: 提取3-5个能够精准概括角色的性格标签（数组格式）。\n" +
                "3. prompt: 一段用于AI角色扮演(Roleplay)的系统提示词(System Prompt)，包含身份定义、说话风格、口癖等，用第二人称'你'来描述。\n" +
                "\n" +
                "请务必严格返回纯 JSON 格式，不要包含 markdown 代码块标记，格式如下：\n" +
                "{\"description\": \"...\", \"tags\": [\"标签1\", \"标签2\"], \"prompt\": \"...\"}";

        String result = callMoonshot(systemPrompt, "角色名：" + name, true);
        return result.replaceAll("```json", "").replaceAll("```", "").trim();
    }

    public JSONObject generateContentAndPrompt(String instruction, String personaName) {
        String systemPrompt = "你是一个社交媒体运营助手。请根据用户指令和角色名(" + personaName + ")，生成两部分内容：\n" +
                "1. content: 符合人设的动态正文(支持Markdown/Emoji)。\n" +
                "2. imagePrompt: 用于AI绘画的英文提示词(Prompt)，描述画面细节。\n" +
                "请务必只返回纯 JSON 格式，不要包含 markdown 标记，格式如下：\n" +
                "{\"content\": \"...\", \"imagePrompt\": \"...\"}";

        String jsonStr = callMoonshot(systemPrompt, instruction, true);
        jsonStr = jsonStr.replaceAll("```json", "").replaceAll("```", "").trim();
        return JSON.parseObject(jsonStr);
    }

    public String generateAndUploadImage(String rawInstruction) {
        String finalPrompt = rawInstruction;
        try {
            log.info("🧠 [Kimi] 正在优化提示词: {}", rawInstruction);
            finalPrompt = optimizePromptWithKimi(rawInstruction);
            log.info("✨ [Kimi] 优化完成: [{}] -> [{}]", rawInstruction, finalPrompt);
        } catch (Exception e) {
            log.warn("⚠️ 提示词优化失败，降级使用原始描述: {}", e.getMessage());
        }

        try {
            String tempUrl = zhipuAiUtils.generateImage(finalPrompt);
            // 下载图片并转存 OSS
            Request request = new Request.Builder().url(tempUrl).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("图片下载失败: " + response.code());
                }
                byte[] imageBytes = response.body().bytes();
                String ossUrl = aliyunOSSOperator.upload(imageBytes, "ai_generated_" + System.currentTimeMillis() + ".png");
                log.info("☁️ [OSS] 图片已转存: {}", ossUrl);
                return ossUrl;
            }
        } catch (Exception e) {
            log.error("❌ 生图或上传失败", e);
            throw new RuntimeException("图片生成服务异常: " + e.getMessage());
        }
    }

    private String optimizePromptWithKimi(String rawInstruction) {
        String systemPrompt = "你是一个精通 AI 绘画的提示词专家。任务是将用户的简单描述转化为 CogView-4 所需的高质量英文提示词。\n" +
                "补充：主体描述、环境背景、光影效果、艺术风格、构图与视角。\n" +
                "要求：仅返回优化后的英文 Prompt，逗号分隔，无前缀。";
        return callMoonshot(systemPrompt, rawInstruction, false);
    }

    public String magicEdit(String originalContent, String personaName, String description, String tags) {
        String systemPrompt = "你是一个精通角色扮演的文案润色大师。你的任务是接收一段普通文本，并将其改写为符合特定角色人设的语气和风格。\n" +
                "--- 角色档案 ---\n" +
                "名字：" + personaName + "\n" +
                "简介：" + (description != null ? description : "无") + "\n" +
                "性格标签：" + (tags != null ? tags : "无") + "\n" +
                "----------------\n" +
                "重要规则（必须遵守）：\n" +
                "1. 【不要重复原文】：直接输出润色后的结果。\n" +
                "2. 【不要解释】：不要说“好的，这是润色后的...”之类的话。\n" +
                "3. 【全覆盖】：润色结果必须包含完整的语义，不要只润色一半。\n" +
                "4. 【风格化】：用词、句式、语气要极度符合该角色的性格特征（例如：傲娇、中二、高冷、古风等）。\n" +
                "5. 【增强表现力】：适当添加 Emoji 表情或颜文字。\n" +
                "\n" +
                "如果不确定如何润色，就保持原文风格但增加一些 Emoji。";

        return callMoonshot(systemPrompt, "请润色这段话：\n" + originalContent, false);
    }

    private String callMoonshot(String systemPrompt, String userContent, boolean jsonMode) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "moonshot-v1-8k");
        requestBody.put("messages", JSONArray.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        requestBody.put("temperature", 0.7);

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Kimi API HTTP " + response.code());
            }
            String jsonStr = response.body().string();
            return JSON.parseObject(jsonStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("Kimi API 调用失败", e);
            throw new RuntimeException("AI 服务暂时不可用: " + e.getMessage());
        }
    }
}