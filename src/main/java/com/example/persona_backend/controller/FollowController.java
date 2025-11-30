package com.example.persona_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.entity.Follow;
import com.example.persona_backend.entity.Persona;
import com.example.persona_backend.mapper.FollowMapper;
import com.example.persona_backend.mapper.PersonaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/follows")
public class FollowController {

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private PersonaMapper personaMapper;

    /**
     * 切换关注状态 (关注/取消关注)
     */
    @PostMapping("/{personaId}")
    public Result<Boolean> toggleFollow(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long personaId) {

        LambdaQueryWrapper<Follow> query = new LambdaQueryWrapper<>();
        query.eq(Follow::getUserId, userId)
                .eq(Follow::getTargetId, personaId);

        Follow existing = followMapper.selectOne(query);

        if (existing != null) {
            // 已关注 -> 取消关注
            followMapper.deleteById(existing.getId());
            return Result.success(false, "Unfollowed");
        } else {
            // 未关注 -> 添加关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setTargetId(personaId);
            follow.setCreatedAt(LocalDateTime.now());

            followMapper.insert(follow);
            return Result.success(true, "Followed");
        }
    }

    /**
     * 查询单个状态
     */
    @GetMapping("/status/{personaId}")
    public Result<Boolean> checkStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long personaId) {

        int count = followMapper.countByUserIdAndTargetId(userId, personaId);
        return Result.success(count > 0);
    }

    /**
     * 获取我关注的智能体列表 (用于聊天列表的关注 Tab)
     */
    @GetMapping("/list")
    public Result<List<Persona>> getFollowedPersonas(@RequestHeader("X-User-Id") Long userId) {

        List<Long> ids = followMapper.selectFollowedPersonaIds(userId);

        if (ids.isEmpty()) {
            return Result.success(List.of());
        }

        return Result.success(personaMapper.selectBatchIds(ids));
    }
}