package com.zaphira.auth.model;

import com.zaphira.common.model.entities.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token - JWT access tokens with revocation support
 */
@Entity
@Table(name = "tokens", indexes = {
    @Index(name = "idx_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_tokens_expired", columnList = "expired"),
    @Index(name = "idx_tokens_revoked", columnList = "revoked")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Builder.Default
    @Column(nullable = false)
    private boolean expired = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
