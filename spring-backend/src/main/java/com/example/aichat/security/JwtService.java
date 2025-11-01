package com.example.aichat.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import java.util.Map;
import java.util.HashMap;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:CHANGEME_SUPER_SECRET_KEY_32_BYTES_MIN_LENGTH_1234567890}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs
    ) {
        // ensure at least 32 bytes for HS256
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            secret = "CHANGEME_SUPER_SECRET_KEY_32_BYTES_MIN_LENGTH_123456";
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject) {
        return generateToken(subject, 0);
    }

    public String generateToken(String subject, int tokenVersion) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("v", tokenVersion);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public int extractVersion(String token) {
        try {
            Object v = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("v");
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) return Integer.parseInt(s);
        } catch (Exception ignore) {}
        return 0;
    }

    public boolean isTokenValid(String token, String username) {
        try {
            String sub = extractUsername(token);
            return username.equals(sub) && !isExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        Date exp = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return exp.before(new Date());
    }
}
