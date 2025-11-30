package com.example.persona_backend.dto;

import com.example.persona_backend.entity.Post;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 详情页专用视图对象 (Value Object)
 * 将 动态本体 + 作者信息 + 互动状态 + 评论列表 打包一次性返回
 */
@Data
public class PostDetailVo {
    // 动态本体
    private Post post;

    // 作者信息 (Persona 或 User)
    private String authorName;
    private String authorAvatar;

    // 当前用户的互动状态
    private Boolean isLiked;      // 我是否点了赞
    private Boolean isBookmarked; // 我是否收藏了

    // 评论列表 (包含用户信息)
    private List<Map<String, Object>> comments;
}