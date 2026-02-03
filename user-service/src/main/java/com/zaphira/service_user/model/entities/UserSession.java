package com.zaphira.service_user.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_session_user_id", columnList = "user_id"),
        @Index(name = "idx_session_token", columnList = "token"),
        @Index(name = "idx_session_refresh_token", columnList = "refresh_token")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(unique = true, length = 500)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Column
    private LocalDateTime refreshExpiryTime;

    @Column
    private LocalDateTime logoutTime;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String deviceType;

    @Column(length = 100)
    private String deviceName;

    @Column(length = 100)
    private String browser;

    @Column(length = 100)
    private String operatingSystem;

    @Column(length = 100)
    private String location;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean isRefreshTokenExpired() {
        return refreshExpiryTime != null && LocalDateTime.now().isAfter(refreshExpiryTime);
    }

    public boolean isValidSession() {
        return isActive && !isExpired();
    }

    public void invalidate() {
        this.isActive = Boolean.FALSE;
        this.logoutTime = LocalDateTime.now();
    }

    public void refreshSession(String newToken, String newRefreshToken, LocalDateTime newExpiryTime) {
        this.token = newToken;
        this.refreshToken = newRefreshToken;
        this.expiryTime = newExpiryTime;
        if (newRefreshToken != null) {
            this.refreshExpiryTime = newExpiryTime.plusDays(7);
        }
    }
}