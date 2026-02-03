package com.zaphira.transaction.model.entities;

import com.zaphira.transaction.model.enums.ComplianceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionRisk - Risk scoring and compliance assessment
 * Normalized table to separate risk analysis from core transaction
 */
@Entity
@Table(name = "transaction_risk", indexes = {
    @Index(name = "idx_transaction_risk_score", columnList = "risk_score"),
    @Index(name = "idx_transaction_risk_compliance", columnList = "compliance_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRisk {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Risk assessment */
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", length = 20)
    private ComplianceStatus complianceStatus;

    @Column(length = 100)
    private String route;

    /** Device context (duplicated for risk analysis) */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}