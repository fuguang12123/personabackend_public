package com.example.persona_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF (前后端分离必须关)
                .csrf(csrf -> csrf.disable())
                // 允许所有请求 (P0/P1 阶段方便调试，后面再加 JWT)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/sync-persona-vectors").permitAll() // 排除该路径的安全检查
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}