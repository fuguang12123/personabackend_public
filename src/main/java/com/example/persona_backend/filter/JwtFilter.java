package com.example.persona_backend.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.persona_backend.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Component
public class JwtFilter implements Filter {

    @Autowired
    private JwtUtils jwtUtils;

    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/upload/image",
            "/error",
            "/admin/sync-persona-vectors"  // 新增白名单路径
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // 1. 放行 OPTIONS 请求 (解决跨域预检) 和 白名单
        if (req.getMethod().equals("OPTIONS") || isWhitelisted(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 获取 Token
        String authHeader = req.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                DecodedJWT jwt = jwtUtils.verifyToken(token);
                Long userId = jwt.getClaim("userId").asLong();

                // 3. 包装 Request，注入 X-User-Id (使用增强版 Wrapper)
                HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(req);
                requestWrapper.addHeader("X-User-Id", userId.toString());

                chain.doFilter(requestWrapper, response);
                return;
            } catch (Exception e) {
                System.out.println("JWT Verify Failed: " + e.getMessage());
            }
        }

        // 3. 验证失败
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\": 401, \"message\": \"Unauthorized: Token Expired or Invalid\", \"data\": null}");
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 增强版 Wrapper：支持 Header 名称忽略大小写查找
     */
    public static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headerMap = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            // 1. 先查自己的 Map (忽略大小写)
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            // 2. 查不到再查原始 Request
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            if (super.getHeaderNames() != null) {
                names = Collections.list(super.getHeaderNames());
            }
            // 把我们新增的 Header 也加进去
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            // 忽略大小写查找
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList(entry.getValue()));
                }
            }
            return super.getHeaders(name);
        }
    }
}