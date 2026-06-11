package com.cartzilla.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Sinh & xác thực JWT. Dùng chung cho user-service (sign) và api-gateway (verify).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTtlMs;

    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret:dev-secret-key-change-me-please-32bytes!!}") String secret,
            @Value("${jwt.access-ttl-ms:900000}") long accessTtlMs) {   // 15 phút
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
    }

    public String generateAccessToken(String userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("email", email, "role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
