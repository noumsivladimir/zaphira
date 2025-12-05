package com.zaphira.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    // ✅ Generate Access Token (keeps existing signature for compatibility)
    public String generateAccessToken(String email) {
        return generateAccessToken(null, email);
    }

    // ✅ Generate Access Token with userId claim
    public String generateAccessToken(Long userId, String email) {
        var builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpiration()));

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    // ✅ Generate Refresh Token
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // ✅ Extract email from token
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ✅ Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (Exception e) {
            System.out.println("❌ JWT validation error: " + e.getMessage());
            return false;
        }
    }
    public long getRefreshExpirationMillis() {
    return jwtProperties.getRefreshExpiration();
}

public String getEmailFromToken(String token) {
    return extractEmail(token);
}

}
