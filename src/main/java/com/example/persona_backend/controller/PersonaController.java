package com.example.persona_backend.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.dto.CreatePostRequest;
import com.example.persona_backend.dto.GenerateImageRequest;
import com.example.persona_backend.dto.MagicEditRequest;
import com.example.persona_backend.dto.PersonaRecommendationDto;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.Post;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.PersonaVectorMapper;
import com.example.persona_backend.mapper.PostMapper;
import com.example.persona_backend.service.AiService;
import com.example.persona_backend.service.RecommendationService;
import com.example.persona_backend.utils.ZhipuAiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/personas")
public class PersonaController {

    @Autowired
    private PersonaMapper personaMapper;

    @Autowired(required = false)
    private PostMapper postMapper;

    @Autowired
    private AiService aiService;
    @Autowired
    private PersonaVectorMapper personaVectorMapper;
    @Autowired
    private ZhipuAiUtils zhipuAiUtils;

    @Autowired
    private RecommendationService recommendationService;

    // ==========================================
    // 广场与推荐
    // ==========================================

    /**
     * [修改] 广场 Feed 接口：支持分页
     * @param page 当前页码，默认 1
     * @param size 每页数量，默认 20
     */
    @GetMapping("/feed")
    public Result<List<Persona>> getFeed(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        LambdaQueryWrapper<Persona> query = new LambdaQueryWrapper<>();
        query.eq(Persona::getIsPublic, true);
        query.orderByDesc(Persona::getCreatedAt);

        // 手动拼接 LIMIT OFFSET 实现分页 (简单且高效)
        // 注意：page 需大于 0
        int offset = (page > 0 ? (page - 1) : 0) * size;
        query.last("LIMIT " + size + " OFFSET " + offset);

        return Result.success(personaMapper.selectList(query));
    }

    /**
     * 独立推荐接口
     */
    @GetMapping("/recommend")
    public Result<List<PersonaRecommendationDto>> getRecommendation(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId
    ) {
        return Result.success(recommendationService.recommendForUser(currentUserId));
    }

    // ==========================================
    // 增删改查 (CRUD)
    // ==========================================

    @PostMapping
    public Result<String> create(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @RequestBody Persona persona) {

        if (persona.getPromptTemplate() == null) {
            persona.setPromptTemplate("You are " + persona.getName());
        }

        persona.setUserId(currentUserId);
        persona.setIsPublic(true);
        persona.setCreatedAt(LocalDateTime.now());

        personaMapper.insert(persona);
        return Result.success("Created");
    }

    @PutMapping("/{id}")
    public Result<String> updatePersona(
            @RequestHeader(value = "X-User-Id") Long currentUserId,
            @PathVariable("id") Long id,
            @RequestBody Persona persona) {

        Persona existing = personaMapper.selectById(id);

        if (existing == null) {
            return Result.error("Persona not found");
        }

        if (!existing.getUserId().equals(currentUserId)) {
            return Result.error("Permission denied: You are not the owner");
        }

        persona.setId(id);
        persona.setUserId(currentUserId);

        int rows = personaMapper.updateById(persona);
        return rows > 0 ? Result.success("Updated") : Result.error("Update failed");
    }

    @GetMapping("/{id}")
    public Result<Persona> getPersona(@PathVariable Long id) {
        return Result.success(personaMapper.selectById(id));
    }

    // ==========================================
    // AI 能力与发帖
    // ==========================================

    @PostMapping("/ai/image")
    public Result<String> generateAiImage(@RequestBody GenerateImageRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isEmpty()) {
            return Result.error("Prompt empty");
        }
        return Result.success(aiService.generateAndUploadImage(request.getPrompt()));
    }

    @PostMapping("/ai/magic-edit")
    public Result<String> magicEdit(@RequestBody MagicEditRequest request) {
        if (request.getContent() == null || request.getContent().isEmpty()) {
            return Result.error("内容不能为空");
        }

        String name = (request.getPersonaName() != null) ? request.getPersonaName() : "热情的社交达人";
        String desc = request.getDescription();
        String tags = request.getTags();

        return Result.success(aiService.magicEdit(request.getContent(), name, desc, tags));
    }

    @PostMapping("/{id}/posts")
    public Result<Map<String, Object>> createPost(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId,
            @PathVariable("id") Long personaId,
            @RequestBody CreatePostRequest request) {

        Persona persona = personaMapper.selectById(personaId);
        if (persona == null) {
            return Result.error("Persona not found");
        }

        Post post = new Post();
        post.setPersonaId(personaId);
        post.setUserId(currentUserId);
        post.setContent(request.getContent());

        String imageUrlsJson = (request.getImageUrls() != null) ? JSON.toJSONString(request.getImageUrls()) : "[]";
        post.setImageUrls(imageUrlsJson);

        post.setLikes(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        if (postMapper != null) {
            postMapper.insert(post);
        }

        JSONObject postJson = (JSONObject) JSON.toJSON(post);
        postJson.put("imageUrls", JSON.parseArray(imageUrlsJson));

        if (post.getCreatedAt() != null) {
            long timestamp = post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            postJson.put("createdAt", timestamp);
        }

        postJson.put("authorName", persona.getName());
        if (persona.getAvatarUrl() != null) {
            postJson.put("authorAvatar", persona.getAvatarUrl());
        }

        return Result.success(postJson);
    }
}