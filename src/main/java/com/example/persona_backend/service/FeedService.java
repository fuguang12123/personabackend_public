package com.example.persona_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.persona_backend.dto.PostDto;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.Post;
import com.example.persona_backend.mapper.PersonaMapper;
import com.example.persona_backend.mapper.PostMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 自动注入 final 字段
public class FeedService {

    private final PostMapper postMapper;
    private final PersonaMapper personaMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取广场动态流 (分页)
     */
    public List<PostDto> getFeedPosts(int page, int size) {
        // 1. 分页查询 Posts 表 (按时间倒序)
        Page<Post> postPage = new Page<>(page, size);
        QueryWrapper<Post> query = new QueryWrapper<>();
        query.orderByDesc("created_at");

        postMapper.selectPage(postPage, query);
        List<Post> posts = postPage.getRecords();

        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取所有涉及的 personaId (批量查询优化，避免 N+1 问题)
        List<Long> personaIds = posts.stream()
                .map(Post::getPersonaId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询 Personas
        List<Persona> personas = personaMapper.selectBatchIds(personaIds);
        // 转为 Map 方便快速查找: id -> Persona
        Map<Long, Persona> personaMap = personas.stream()
                .collect(Collectors.toMap(Persona::getId, p -> p));

        // 4. 组装 DTO (Entity -> DTO)
        return posts.stream().map(post -> {
            PostDto dto = new PostDto();
            dto.setId(post.getId());
            dto.setPersonaId(post.getPersonaId().toString());
            dto.setContent(post.getContent());
            dto.setLikes(post.getLikes());
            // 转换时间为时间戳
            dto.setCreatedAt(post.getCreatedAt().toInstant(ZoneOffset.of("+8")).toEpochMilli());
            dto.setIsLiked(false); // Day 7 简化：暂时默认为未点赞

            // 解析 JSON 图片列表
            try {
                if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                    List<String> urls = objectMapper.readValue(post.getImageUrls(), new TypeReference<List<String>>(){});
                    dto.setImageUrls(urls);
                } else {
                    dto.setImageUrls(Collections.emptyList());
                }
            } catch (Exception e) {
                dto.setImageUrls(Collections.emptyList());
            }

            // 填充作者信息 (快照)
            Persona author = personaMap.get(post.getPersonaId());
            if (author != null) {
                dto.setAuthorName(author.getName());
                dto.setAuthorAvatar(author.getAvatarUrl()); // 假设 Persona 实体有 getAvatar()
            } else {
                // 兜底数据
                dto.setAuthorName("未知智能体");
                dto.setAuthorAvatar("https://api.dicebear.com/7.x/initials/png?seed=U");
            }

            return dto;
        }).collect(Collectors.toList());
    }
}