package com.example.persona_backend.controller;

import com.example.persona_backend.common.Result;
import com.example.persona_backend.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Autowired private AiService aiService;

    @PostMapping("/generate-persona")
    public Result<String> generate(@RequestBody Map<String, String> params) {
        String name = params.get("name");
        if (name == null) return Result.error("Name required");
        return Result.success(aiService.generatePersonaDescription(name));
    }
}
