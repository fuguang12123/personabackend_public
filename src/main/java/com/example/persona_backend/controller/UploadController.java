package com.example.persona_backend.controller;

import com.example.persona_backend.common.Result;
import com.example.persona_backend.utils.AliyunOSSOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload") // ✅ 简化路径
public class UploadController {

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;


    @PostMapping("/image")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传失败：文件为空");
        }
        try {
            byte[] bytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            String url = aliyunOSSOperator.upload(bytes, originalFilename);

            return Result.success(url);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
