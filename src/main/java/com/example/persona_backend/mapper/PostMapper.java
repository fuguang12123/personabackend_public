package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    // 🔥 [新增] 联表查询：查询我关注的 Persona 发的帖子
    @Select("SELECT p.* FROM posts p " +
            "JOIN follow f ON p.persona_id = f.target_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY p.created_at DESC")
    List<Post> selectFollowedPosts(Long userId);
}