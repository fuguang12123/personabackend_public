package com.example.persona_backend.utils;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.core.Constants;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import okhttp3.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 智谱 AI (BigModel) 工具类 - 最终修复版
 * 核心能力: 调用 CogView-4 生成图片
 */
@Component
public class ZhipuAiUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZhipuAiUtils.class);

    @Value("${zhipu.api.key:}")
    private String apiKey;

    private ZhipuAiClient client;
    private final OkHttpClient httpClient = new OkHttpClient();

    private static final String EMBEDDING_URL = "https://open.bigmodel.cn/api/paas/v4/embeddings";
    // 指定使用最新的 CogView-4 模型
    private static final String MODEL_NAME = Constants.ModelCogView4250304;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.client = ZhipuAiClient.builder()
                    .apiKey(apiKey)
                    .build();
            logger.info("✅ [ZhipuAiUtils] Client initialized with Model: {}", MODEL_NAME);
        } else {
            logger.warn("⚠️ [ZhipuAiUtils] API Key 未配置，生图功能将不可用");
        }
    }

    public String generateImage(String prompt) {
        if (client == null) {
            logger.warn("⚠️ [Mock Mode] API Key missing.");
            return "https://picsum.photos/1024/1024?random=" + System.currentTimeMillis();
        }

        try {
            logger.info("🚀 [CogView-4] 开始生图, Prompt: {}", prompt);

            CreateImageRequest request = CreateImageRequest.builder()
                    .model(MODEL_NAME)
                    .prompt(prompt)
                    .size("1024x1024")
                    .build();

            ImageResponse response = client.images().createImage(request);

            if (response != null && response.getData() != null) {
                Object resultData = response.getData();
                String jsonString = JSON.toJSONString(resultData);
                logger.info("🔍 [Debug] Zhipu Raw JSON: {}", jsonString);

                JSONObject jsonObject = JSON.parseObject(jsonString);

                // === 修复策略: 优先检查深层结构 ===

                // 1. 检查是否存在 'data' 数组 (符合日志结构)
                if (jsonObject.containsKey("data")) {
                    JSONArray dataArray = jsonObject.getJSONArray("data");
                    if (dataArray != null && !dataArray.isEmpty()) {
                        JSONObject firstItem = dataArray.getJSONObject(0);
                        if (firstItem.containsKey("url")) {
                            String url = firstItem.getString("url");
                            logger.info("✅ [Strategy Nested] 成功提取深层URL: {}", url);
                            return url;
                        }
                    }
                }

                // 2. 检查根节点是否有 'url' (兼容旧结构)
                if (jsonObject.containsKey("url")) {
                    return jsonObject.getString("url");
                }

                // 3. 反射兜底 (仅扫描 String 字段)
                for (Field field : resultData.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(resultData);
                        if (value instanceof String && ((String) value).startsWith("http")) {
                            logger.info("✅ [Strategy Reflect] 反射提取成功: {}", value);
                            return (String) value;
                        }
                    } catch (IllegalAccessException e) {
                        // ignore
                    }
                }
            }

            throw new RuntimeException("Zhipu API structure mismatch. JSON: " + JSON.toJSONString(response.getData()));

        } catch (Exception e) {
            logger.error("❌ [ZhipuAiUtils] SDK 调用失败", e);
            throw new RuntimeException("Image generation failed: " + e.getMessage());
        }
    }
    public List<Double> generateEmbedding(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("API Key missing for embedding");
            return new ArrayList<>();
        }

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "embedding-2");
            jsonBody.put("input", text);

            Request request = new Request.Builder()
                    .url(EMBEDDING_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Embedding API error: {}", response.code());
                    return new ArrayList<>();
                }

                String resStr = response.body().string();
                JSONObject resJson = JSON.parseObject(resStr);

                // 智谱 API 返回结构: data[0].embedding
                if (resJson.containsKey("data")) {
                    JSONArray data = resJson.getJSONArray("data");
                    if (data != null && !data.isEmpty()) {
                        return data.getJSONObject(0).getList("embedding", Double.class);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Embedding generation failed", e);
        }
        return new ArrayList<>();
    }
}