package com.example.hcp.global.security;

import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-seconds}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-token-ttl-seconds}") long refreshTtlSeconds
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 chars");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = accessTtlSeconds * 1000L;
        this.refreshTtlMillis = refreshTtlSeconds * 1000L;
    }

    public String createAccessToken(Long userId, Role role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTtlMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTtlMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long validateAndGetUserIdFromRefreshToken(String token) {
        Claims c = parseClaims(token);
        String typ = c.get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
        return Long.parseLong(c.getSubject());
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_TOKEN");
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public Role getRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        if (role == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_TOKEN");
        }
        return Role.valueOf(role);
    }
}
