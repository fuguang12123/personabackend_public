package com.example.persona_backend.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.dto.PostDto;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.entity.Post;
import com.example.persona_backend.entity.User;
import com.example.persona_backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired private UserMapper userMapper;
    @Autowired private PostMapper postMapper;
    @Autowired private PersonaMapper personaMapper;

    @GetMapping("/me")
    public Result<User> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/me")
    public Result<User> updateProfile(@RequestHeader("X-User-Id") Long userId, @RequestBody User userUpdate) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("User not found");

        if (userUpdate.getAvatarUrl() != null) user.setAvatarUrl(userUpdate.getAvatarUrl());
        if (userUpdate.getBackgroundImageUrl() != null) user.setBackgroundImageUrl(userUpdate.getBackgroundImageUrl());
        if (userUpdate.getNickname() != null) user.setNickname(userUpdate.getNickname());

        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/me/password")
    public Result<String> changePassword(@RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, String> body) {
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");

        User user = userMapper.selectById(userId);
        if (!user.getPassword().equals(oldPwd)) {
            return Result.error("旧密码错误");
        }
        user.setPassword(newPwd);
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    // ================= 列表查询 =================

    @GetMapping("/me/personas")
    public Result<List<Persona>> getMyPersonas(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(personaMapper.selectList(
                new LambdaQueryWrapper<Persona>().eq(Persona::getUserId, userId).orderByDesc(Persona::getCreatedAt)
        ));
    }

    @GetMapping("/me/posts")
    public Result<List<PostDto>> getMyPosts(@RequestHeader("X-User-Id") Long userId) {
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getUserId, userId).orderByDesc(Post::getCreatedAt)
        );
        return Result.success(convertToDto(posts));
    }

    @GetMapping("/me/likes")
    public Result<List<PostDto>> getMyLikes(@RequestHeader("X-User-Id") Long userId) {
        // 使用子查询获取我点赞的帖子
        List<Post> posts = postMapper.selectList(
                new QueryWrapper<Post>().inSql("id", "SELECT post_id FROM post_likes WHERE user_id = " + userId)
                        .orderByDesc("created_at")
        );
        return Result.success(convertToDto(posts));
    }

    @GetMapping("/me/bookmarks")
    public Result<List<PostDto>> getMyBookmarks(@RequestHeader("X-User-Id") Long userId) {
        List<Post> posts = postMapper.selectList(
                new QueryWrapper<Post>().inSql("id", "SELECT post_id FROM post_bookmarks WHERE user_id = " + userId)
                        .orderByDesc("created_at")
        );
        return Result.success(convertToDto(posts));
    }

    // ✅ [Fix] 完善的 DTO 转换，确保列表页显示作者头像
    private List<PostDto> convertToDto(List<Post> posts) {
        if (posts.isEmpty()) return Collections.emptyList();

        // 1. 批量查询关联的 Persona
        List<Long> personaIds = posts.stream().map(Post::getPersonaId).distinct().collect(Collectors.toList());
        List<Persona> personas = personaMapper.selectBatchIds(personaIds);
        Map<Long, Persona> personaMap = personas.stream().collect(Collectors.toMap(Persona::getId, p -> p));

        // 2. 组装
        return posts.stream().map(post -> {
            PostDto dto = new PostDto();
            dto.setId(post.getId());
            dto.setPersonaId(post.getPersonaId().toString());
            dto.setContent(post.getContent());
            dto.setLikes(post.getLikes());
            dto.setCreatedAt(post.getCreatedAt().toInstant(ZoneOffset.of("+8")).toEpochMilli());
            dto.setIsLiked(true); // 在“我的点赞”列表中，这些肯定都是 liked，但在“我的动态”中不一定。这里简化处理。

            try {
                if (post.getImageUrls() != null && !post.getImageUrls().isEmpty() && !post.getImageUrls().equals("[]")) {
                    List<String> urls = JSON.parseArray(post.getImageUrls(), String.class);
                    dto.setImageUrls(urls);
                } else {
                    dto.setImageUrls(Collections.emptyList());
                }
            } catch (Exception e) {
                dto.setImageUrls(Collections.emptyList());
            }

            Persona author = personaMap.get(post.getPersonaId());
            if (author != null) {
                dto.setAuthorName(author.getName());
                dto.setAuthorAvatar(author.getAvatarUrl());
            } else {
                dto.setAuthorName("Unknown");
            }
            return dto;
        }).collect(Collectors.toList());
    }
}