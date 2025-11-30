package com.example.persona_backend.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.dto.PostDto;
import com.example.persona_backend.entity.*;
import com.example.persona_backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired private PostMapper postMapper;
    @Autowired private PostCommentMapper commentMapper;
    @Autowired private PostLikeMapper likeMapper;
    @Autowired private PostBookmarkMapper bookmarkMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private PersonaMapper personaMapper;
    @Autowired private UserMapper userMapper;

    // ==========================================
    // 🔥 动态流 (Feed) 接口
    // ==========================================

    @GetMapping("/feed")
    public Result<List<PostDto>> getFeed(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "type", required = false, defaultValue = "all") String type) {

        List<Post> posts;

        // 1. 查询帖子数据
        if ("followed".equals(type) && userId != null) {
            // 关注流：走自定义 SQL 联表查询
            posts = postMapper.selectFollowedPosts(userId);
        } else {
            // 广场流：查询所有，按时间倒序
            posts = postMapper.selectList(
                    new LambdaQueryWrapper<Post>()
                            .orderByDesc(Post::getCreatedAt)
                            .last("LIMIT 50")
            );
        }

        if (posts == null || posts.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 准备批量查询的 ID 集合 (避免循环查库)
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Set<Long> personaIds = posts.stream()
                .map(Post::getPersonaId)
                .collect(Collectors.toSet());

        // 3. 批量查询作者信息 (Persona)
        Map<Long, Persona> personaMap = new HashMap<>();
        if (!personaIds.isEmpty()) {
            List<Persona> personas = personaMapper.selectBatchIds(personaIds);
            personaMap = personas.stream()
                    .collect(Collectors.toMap(Persona::getId, p -> p));
        }

        // 4. 批量查询交互状态 (点赞/收藏)
        Set<Long> likedPostIds = new HashSet<>();
        Set<Long> bookmarkedPostIds = new HashSet<>();

        if (userId != null && !postIds.isEmpty()) {
            // 查询点赞
            List<PostLike> likes = likeMapper.selectList(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getUserId, userId)
                            .in(PostLike::getPostId, postIds)
            );
            likedPostIds = likes.stream()
                    .map(PostLike::getPostId)
                    .collect(Collectors.toSet());

            // 查询收藏
            List<PostBookmark> bookmarks = bookmarkMapper.selectList(
                    new LambdaQueryWrapper<PostBookmark>()
                            .eq(PostBookmark::getUserId, userId)
                            .in(PostBookmark::getPostId, postIds)
            );
            bookmarkedPostIds = bookmarks.stream()
                    .map(PostBookmark::getPostId)
                    .collect(Collectors.toSet());
        }

        // 5. 组装最终 DTO 列表
        List<PostDto> dtos = new ArrayList<>();

        for (Post p : posts) {
            PostDto dto = new PostDto();

            // 基础信息
            dto.setId(p.getId());
            dto.setPersonaId(String.valueOf(p.getPersonaId()));
            dto.setUserId(p.getUserId());
            dto.setContent(p.getContent());
            dto.setLikes(p.getLikes());

            // 图片解析
            try {
                if (p.getImageUrls() != null) {
                    dto.setImageUrls(JSON.parseArray(p.getImageUrls(), String.class));
                }
            } catch (Exception e) {
                dto.setImageUrls(new ArrayList<>());
            }

            // 时间转换
            if (p.getCreatedAt() != null) {
                dto.setCreatedAt(p.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli());
            }

            // 填充作者信息
            Persona persona = personaMap.get(p.getPersonaId());
            if (persona != null) {
                dto.setAuthorName(persona.getName());
                dto.setAuthorAvatar(persona.getAvatarUrl());
            } else {
                dto.setAuthorName("Unknown");
            }

            // 填充交互状态
            dto.setIsLiked(likedPostIds.contains(p.getId()));
            dto.setIsBookmarked(bookmarkedPostIds.contains(p.getId()));

            dtos.add(dto);
        }

        return Result.success(dtos);
    }

    // ==========================================
    // 🔔 通知相关接口
    // ==========================================

    @GetMapping("/notifications/unread-count")
    public Result<Long> getUnreadCount(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsRead, false)
        );
        return Result.success(count);
    }

    @PostMapping("/notifications/read")
    public Result<String> markAllAsRead(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        Notification update = new Notification();
        update.setIsRead(true);

        notificationMapper.update(update,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsRead, false)
        );
        return Result.success("Marked as read");
    }

    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> getNotifications(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        List<Notification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUserId)
                        .orderByDesc(Notification::getCreatedAt)
        );

        if (notifications.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 收集发送者 ID
        List<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> avatarMap = new HashMap<>();

        if (!senderIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(senderIds);
            for (User u : users) {
                nameMap.put(u.getId(), u.getUsername());
                avatarMap.put(u.getId(), "https://api.dicebear.com/7.x/avataaars/png?seed=" + u.getUsername());
            }
        }

        // 组装通知详情
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", n.getId());
            vo.put("type", n.getType());
            vo.put("postId", n.getTargetId());
            vo.put("isRead", n.getIsRead());

            if (n.getCreatedAt() != null) {
                vo.put("createdAt", n.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }

            Long sid = n.getSenderId();
            vo.put("senderName", nameMap.getOrDefault(sid, "未知用户"));
            vo.put("senderAvatar", avatarMap.getOrDefault(sid, ""));

            resultList.add(vo);
        }
        return Result.success(resultList);
    }

    // ==========================================
    // 📖 帖子详情 (包含 Map 组装逻辑)
    // ==========================================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getPostDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        Post post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("Post not found");
        }

        Persona persona = personaMapper.selectById(post.getPersonaId());

        Long likeCount = likeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, id)
                        .eq(PostLike::getUserId, currentUserId)
        );

        Long bookmarkCount = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<PostBookmark>()
                        .eq(PostBookmark::getPostId, id)
                        .eq(PostBookmark::getUserId, currentUserId)
        );

        var comments = commentMapper.selectCommentsWithUser(id);

        Map<String, Object> postDto = new HashMap<>();
        postDto.put("id", post.getId());
        postDto.put("userId", post.getUserId());
        postDto.put("personaId", post.getPersonaId());
        postDto.put("content", post.getContent());

        try {
            postDto.put("imageUrls", JSON.parseArray(post.getImageUrls()));
        } catch (Exception e) {
            postDto.put("imageUrls", new String[]{});
        }

        postDto.put("likes", post.getLikes());

        if (post.getCreatedAt() != null) {
            postDto.put("createdAt", post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("post", postDto);
        response.put("isLiked", likeCount > 0);
        response.put("isBookmarked", bookmarkCount > 0);
        response.put("comments", comments);

        if (persona != null) {
            response.put("authorName", persona.getName());
            response.put("authorAvatar", persona.getAvatarUrl());
        } else {
            response.put("authorName", "Unknown");
        }
        return Result.success(response);
    }

    // ==========================================
    // 👍 交互操作 (事务与通知)
    // ==========================================

    @PostMapping("/{id}/like")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> toggleLike(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        Post post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("Post not found");
        }

        PostLike existingLike = likeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, id)
                        .eq(PostLike::getUserId, currentUserId)
        );

        boolean isLiked;
        if (existingLike != null) {
            // 取消点赞
            likeMapper.deleteById(existingLike.getId());
            post.setLikes(Math.max(0, post.getLikes() - 1));
            isLiked = false;

            // 撤回通知
            notificationMapper.delete(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getType, 1)
                            .eq(Notification::getSenderId, currentUserId)
                            .eq(Notification::getTargetId, id)
                            .eq(Notification::getReceiverId, post.getUserId())
            );
        } else {
            // 点赞
            PostLike newLike = new PostLike();
            newLike.setPostId(id);
            newLike.setUserId(currentUserId);
            newLike.setCreatedAt(LocalDateTime.now());

            likeMapper.insert(newLike);
            post.setLikes(post.getLikes() + 1);
            isLiked = true;

            // 发送通知 (不通知自己)
            if (!post.getUserId().equals(currentUserId)) {
                createNotification(post.getUserId(), currentUserId, 1, id);
            }
        }

        postMapper.updateById(post);
        return Result.success(isLiked ? "Liked" : "Unliked");
    }

    @PostMapping("/{id}/bookmark")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> toggleBookmark(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        Post post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("Post not found");
        }

        PostBookmark existingBm = bookmarkMapper.selectOne(
                new LambdaQueryWrapper<PostBookmark>()
                        .eq(PostBookmark::getPostId, id)
                        .eq(PostBookmark::getUserId, currentUserId)
        );

        boolean isBookmarked;
        if (existingBm != null) {
            bookmarkMapper.deleteById(existingBm.getId());
            isBookmarked = false;
        } else {
            PostBookmark newBm = new PostBookmark();
            newBm.setPostId(id);
            newBm.setUserId(currentUserId);
            newBm.setCreatedAt(LocalDateTime.now());

            bookmarkMapper.insert(newBm);
            isBookmarked = true;
        }
        return Result.success(isBookmarked ? "Bookmarked" : "Unbookmarked");
    }

    @PostMapping("/{id}/comments")
    @Transactional(rollbackFor = Exception.class)
    public Result<PostComment> addComment(
            @PathVariable Long id,
            @RequestBody PostComment comment,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long currentUserId) {

        comment.setPostId(id);
        comment.setUserId(currentUserId);
        comment.setCreatedAt(LocalDateTime.now());

        Long receiverId = null;
        int notificationType = 2;

        if (comment.getParentId() != null) {
            // 回复评论
            PostComment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                Long rootId = parent.getRootParentId() != null ? parent.getRootParentId() : parent.getId();
                comment.setRootParentId(rootId);
                comment.setReplyToUserId(parent.getUserId());

                receiverId = parent.getUserId();
                notificationType = 3; // 3=回复
            }
        } else {
            // 评论帖子
            Post post = postMapper.selectById(id);
            if (post != null) {
                receiverId = post.getUserId();
                notificationType = 2; // 2=评论
            }
        }

        commentMapper.insert(comment);

        if (receiverId != null && !receiverId.equals(currentUserId)) {
            createNotification(receiverId, currentUserId, notificationType, id);
        }

        return Result.success(comment);
    }

    // 辅助方法：创建通知
    private void createNotification(Long receiverId, Long senderId, int type, Long targetId) {
        Notification n = new Notification();
        n.setReceiverId(receiverId);
        n.setSenderId(senderId);
        n.setType(type);
        n.setTargetId(targetId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }
}