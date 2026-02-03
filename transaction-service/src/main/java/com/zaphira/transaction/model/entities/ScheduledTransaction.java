package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_transactions", indexes = {
        @Index(name = "idx_scheduled_status", columnList = "status"),
        @Index(name = "idx_scheduled_due", columnList = "scheduled_for")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "run_count", nullable = false)
    @Builder.Default
    private Integer runCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Lob
    @Column(name = "request_payload", nullable = false)
    private String requestPayload;

    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @Column(name = "requester_email")
    private String requesterEmail;

    @Column(name = "requester_roles")
    private String requesterRoles;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
