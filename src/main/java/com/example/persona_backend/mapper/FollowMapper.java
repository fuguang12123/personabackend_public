package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
    // Check if followed
    @Select("SELECT COUNT(*) FROM follow WHERE user_id = #{userId} AND target_id = #{targetId}")
    int countByUserIdAndTargetId(Long userId, Long targetId);

    // Get the list of Persona IDs followed by the user
    @Select("SELECT target_id FROM follow WHERE user_id = #{userId}")
    List<Long> selectFollowedPersonaIds(Long userId);
}