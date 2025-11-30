package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 这里不需要写任何代码！
    // BaseMapper 已经帮你实现了 insert, selectById, selectOne 等方法
}