package com.example.persona_backend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {
    // ⚠️ 真实生产环境请将密钥放入配置文件
    private static final String SECRET_KEY = "PersonaAppSecretKeyForSigningJwtTokensMustBeLongEnough";
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 30; // 30天过期

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username) {
        // HMAC256 需要密钥
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

        return JWT.create()
                .withSubject(username)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(algorithm);
    }

    /**
     * 验证 Token 并返回解码后的对象
     * 如果验证失败（过期、篡改），会抛出异常 (JWTVerificationException)
     */
    public DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    /**
     * 从 Token 中获取 UserID (不验证，仅解析)
     * 通常我们在 verifyToken 成功后直接从 DecodedJWT 取即可，这个方法作为备用
     */
    public Long getUserId(DecodedJWT jwt) {
        return jwt.getClaim("userId").asLong();
    }
}