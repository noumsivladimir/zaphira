package com.zaphira.transaction.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionFees - Fee breakdown and calculations
 * Normalized table to separate fee concerns from core transaction
 */
@Entity
@Table(name = "transaction_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFees {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Fee details */
    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "fee_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_currency", length = 10)
    private String feeCurrency;

    @Column(name = "fee_type", length = 50)
    private String feeType;

    @Column(name = "service_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    /** Calculated amounts */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount;

    /** Foreign exchange fees */
    @Column(name = "fx_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fxFee = BigDecimal.ZERO;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "fx_rate", precision = 19, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "fx_rate_provider", length = 50)
    private String fxRateProvider;

    /** Audit */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Calculate total amount from base amount + fees
     */
    public void calculateTotalAmount(BigDecimal baseAmount) {
        this.totalAmount = baseAmount
            .add(fee != null ? fee : BigDecimal.ZERO)
            .add(serviceFee != null ? serviceFee : BigDecimal.ZERO);
    }

    /**
     * Calculate net amount after all deductions
     */
    public void calculateNetAmount(BigDecimal baseAmount) {
        this.netAmount = baseAmount
            .subtract(fee != null ? fee : BigDecimal.ZERO)
            .subtract(serviceFee != null ? serviceFee : BigDecimal.ZERO)
            .subtract(fxFee != null ? fxFee : BigDecimal.ZERO);
    }
}
