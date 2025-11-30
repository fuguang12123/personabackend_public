package com.example.persona_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.persona_backend.common.Result;
import com.example.persona_backend.entity.User;
import com.example.persona_backend.mapper.UserMapper;
import com.example.persona_backend.utils.CaptchaUtils;
import com.example.persona_backend.utils.JwtUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private UserMapper userMapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private CaptchaUtils captchaUtils;

    private static final Map<String, String[]> CAPTCHA_STORE = new ConcurrentHashMap<>();

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String confirmPassword;
        private String captchaUuid;
        private String captchaCode;
    }

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() throws Exception {
        String code = captchaUtils.generateCode();
        String uuid = UUID.randomUUID().toString();
        CAPTCHA_STORE.put(uuid, new String[]{code, String.valueOf(System.currentTimeMillis())});
        BufferedImage image = captchaUtils.generateImage(code);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        String base64Img = "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        Map<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("image", base64Img);
        if (CAPTCHA_STORE.size() > 1000) CAPTCHA_STORE.clear();
        return Result.success(map);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, req.getUsername());
        User user = userMapper.selectOne(query);
        if (user == null || !user.getPassword().equals(req.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId().toString());
        data.put("username", user.getUsername());
        data.put("avatarUrl", user.getAvatarUrl());
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        if (req.getUsername() == null || req.getPassword() == null) return Result.error("用户名密码不能为空");
        if (!req.getPassword().equals(req.getConfirmPassword())) return Result.error("两次密码输入不一致");
        if (req.getCaptchaUuid() == null || req.getCaptchaCode() == null) return Result.error("请输入验证码");
        String[] stored = CAPTCHA_STORE.get(req.getCaptchaUuid());
        if (stored == null) return Result.error("验证码已失效");
        if (!stored[0].equalsIgnoreCase(req.getCaptchaCode())) return Result.error("验证码错误");
        CAPTCHA_STORE.remove(req.getCaptchaUuid());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) return Result.error("用户名已存在");

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setCreatedAt(LocalDateTime.now());
        user.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/png?seed=" + req.getUsername());
        userMapper.insert(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId().toString());
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    // ✅ [New] Token 自动续期接口
    // 原理：前端带旧 Token 访问，JwtFilter 验证通过后注入 X-User-Id，
    // 这里直接签发一个新 Token 返回。
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestHeader("X-User-Id") Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("User not found");

        String newToken = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        data.put("userId", user.getId().toString());

        return Result.success(data);
    }
}