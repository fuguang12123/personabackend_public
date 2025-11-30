package com.example.persona_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.dto.PostDto; // 假设你有这个DTO，或者直接返回Map
import com.example.persona_backend.entity.Post;
import com.example.persona_backend.entity.PostLike;
import com.example.persona_backend.entity.PostBookmark;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.mapper.PostMapper;
import com.example.persona_backend.mapper.PostLikeMapper;
import com.example.persona_backend.mapper.PostBookmarkMapper;
import com.example.persona_backend.mapper.PersonaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/feed")
public class FeedController {

    @Autowired private PostMapper postMapper;
    @Autowired private PostLikeMapper likeMapper;
    @Autowired private PostBookmarkMapper bookmarkMapper;
    @Autowired private PersonaMapper personaMapper;

    /**
     * 获取动态流 (修复了点赞状态不正确的问题)
     */
    @GetMapping("/posts")
    public Result<List<Map<String, Object>>> getFeedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long currentUserId) {

        // 1. 分页查询 Post
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();
        query.orderByDesc(Post::getCreatedAt);
        postMapper.selectPage(postPage, query);
        List<Post> posts = postPage.getRecords();

        if (posts.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 批量获取 ID
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

        // 3. 批量查询当前用户的点赞状态
        List<Long> likedPostIds;
        if (currentUserId > 0) {
            LambdaQueryWrapper<PostLike> likeQuery = new LambdaQueryWrapper<>();
            likeQuery.in(PostLike::getPostId, postIds).eq(PostLike::getUserId, currentUserId);
            likedPostIds = likeMapper.selectList(likeQuery).stream()
                    .map(PostLike::getPostId).collect(Collectors.toList());
        } else {
            likedPostIds = new ArrayList<>();
        }

        // 4. 批量查询当前用户的收藏状态
        List<Long> bookmarkedPostIds;
        if (currentUserId > 0) {
            LambdaQueryWrapper<PostBookmark> bmQuery = new LambdaQueryWrapper<>();
            bmQuery.in(PostBookmark::getPostId, postIds).eq(PostBookmark::getUserId, currentUserId);
            bookmarkedPostIds = bookmarkMapper.selectList(bmQuery).stream()
                    .map(PostBookmark::getPostId).collect(Collectors.toList());
        } else {
            bookmarkedPostIds = new ArrayList<>();
        }

        // 5. 组装 DTO
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("personaId", post.getPersonaId());
            map.put("content", post.getContent());
            // 解析 JSON 字符串为对象 (如果后端框架没自动做，这里手动处理一下，这里假设前端能处理)
            map.put("imageUrls", com.alibaba.fastjson2.JSON.parseArray(post.getImageUrls()));
            map.put("likes", post.getLikes());
            map.put("createdAt", post.getCreatedAt());

            // 注入互动状态
            map.put("isLiked", likedPostIds.contains(post.getId()));
            map.put("isBookmarked", bookmarkedPostIds.contains(post.getId()));

            // 注入作者信息
            Persona persona = personaMapper.selectById(post.getPersonaId());
            if (persona != null) {
                map.put("authorName", persona.getName());
                map.put("authorAvatar", persona.getAvatarUrl());
            } else {
                map.put("authorName", "Unknown");
            }

            resultList.add(map);
        }

        return Result.success(resultList);
    }
}