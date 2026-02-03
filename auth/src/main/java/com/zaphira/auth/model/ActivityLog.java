package com.zaphira.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import com.zaphira.common.model.entities.User;

/**
 * ActivityLog - Complete audit log of user activities
 */
@Entity
@Table(name = "activity_log", indexes = {
    @Index(name = "idx_activity_log_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_activity_log_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_user_id")
    private Long userUserId; // Duplicate field for compatibility

    @Column(nullable = false, length = 255)
    private String action;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Additional context */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 255)
    private String resource;

    @Column(length = 10)
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    /** Request/Response details */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (user != null && userUserId == null) {
            userUserId = user.getUserId();
        }
    }
}
