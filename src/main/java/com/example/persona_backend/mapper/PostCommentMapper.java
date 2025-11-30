package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {

    // [Fix] 显式格式化 created_at 为字符串，防止 JSON 解析错误
    @Select("SELECT c.id, c.post_id, c.user_id, c.content, " +
            "c.root_parent_id, c.parent_id, c.reply_to_user_id, " +
            "DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s') as created_at, " +
            "CONCAT('User ', c.user_id) as user_name, " +
            "CONCAT('https://api.dicebear.com/7.x/avataaars/png?seed=', c.user_id) as user_avatar " +
            "FROM post_comments c " +
            "WHERE c.post_id = #{postId} " +
            "ORDER BY c.created_at DESC")
    List<Map<String, Object>> selectCommentsWithUser(Long postId);
}