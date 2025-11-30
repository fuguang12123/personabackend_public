package com.example.persona_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.persona_backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}