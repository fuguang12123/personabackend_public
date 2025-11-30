package com.example.persona_backend.controller;

import com.example.persona_backend.common.Result;
import com.example.persona_backend.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private AiService aiService;

    /**
     * 🧪 测试接口：仅测试 "生图 + 上传OSS" 链路
     * 对应前端用户点击 "AI 配图" 按钮的动作
     * * URL: http://localhost:8080/test/generate-image?prompt=一只可爱的猫
     */
    @GetMapping("/generate-image")
    public Result<String> testGenerateImage(@RequestParam String prompt) {
        try {
            System.out.println("🧪 [测试] 收到生图请求: " + prompt);

            // 1. 调用 Zhipu 生成临时链接
            // 2. 下载并上传阿里云 OSS
            String ossUrl = aiService.generateAndUploadImage(prompt);

            System.out.println("✅ [测试] 图片已上传 OSS: " + ossUrl);
            return Result.success(ossUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生图测试失败: " + e.getMessage());
        }
    }
}