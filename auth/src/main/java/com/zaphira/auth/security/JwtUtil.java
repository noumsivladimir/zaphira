package com.zaphira.auth.security;

import com.zaphira.common.model.enums.RoleType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    // ✅ Generate Access Token (keeps existing signature for compatibility)
    public String generateAccessToken(String email) {
        return generateAccessToken(null, email, null);
    }

    // ✅ Backward-compatible helper (no role)
    public String generateAccessToken(Long userId, String email) {
        return generateAccessToken(userId, email, null);
    }

    // ✅ Generate Access Token with userId claim and roles
    public String generateAccessToken(Long userId, String email, RoleType role) {
        var builder = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpiration()));

        if (userId != null) {
            builder.claim("userId", userId);
            
            // For MERCHANT users, also include merchantId (same as userId)
            if (role == RoleType.MERCHANT) {
                builder.claim("merchantId", userId);
            }
        }

        if (role != null) {
            builder.claim("roles", Collections.singletonList(role.name()));
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

    // ✅ Extract roles (may be absent on legacy tokens)
    public List<String> extractRoles(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object roles = claims.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream()
                        .map(Object::toString)
                        .toList();
            }
            if (roles instanceof String single) {
                return List.of(single);
            }
        } catch (Exception ignored) {
            // fall through to empty list
        }
        return List.of();
    }

    // ✅ Extract userId from token
    public Long extractUserId(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object userId = claims.get("userId");
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        } catch (Exception ignored) {
            // fall through to null
        }
        return null;
    }

    // ✅ Extract merchantId from token (only present for MERCHANT role)
    public Long extractMerchantId(String token) {
        try {
            var claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object merchantId = claims.get("merchantId");
            if (merchantId != null) {
                return Long.valueOf(merchantId.toString());
            }
        } catch (Exception ignored) {
            // fall through to null
        }
        return null;
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
