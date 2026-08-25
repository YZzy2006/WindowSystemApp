package com.window.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private SecretKey secretKey;
    private long expireMs;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Value("${jwt.expire-hours}")
    public void setExpireHours(long hours) {
        this.expireMs = hours * 3600 * 1000L;
    }

    public String createToken(String adminId, String tokenId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(adminId)
                .claim("tokenId", tokenId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public String validateAndGetAdminId(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

}
