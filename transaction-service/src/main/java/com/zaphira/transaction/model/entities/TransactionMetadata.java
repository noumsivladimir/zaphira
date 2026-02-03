package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TransactionMetadata - Device, IP, and additional metadata
 * Normalized table to separate contextual information from core transaction
 */
@Entity
@Table(name = "transaction_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionMetadata {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Device and channel info */
    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** Additional metadata (JSON) */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** Failure information */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}