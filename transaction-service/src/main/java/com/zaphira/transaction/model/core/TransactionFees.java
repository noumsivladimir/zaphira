package com.zaphira.transaction.model.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionFees - LOT 2
 * 
 * Stores fee breakdown for transactions:
 * - Platform fee (charged by platform)
 * - Merchant fee (charged to merchant)
 * - Total fees
 * 
 * Fee Calculation Rules:
 * - TRANSFER: 0.5% platform fee
 * - MERCHANT_PAYMENT: 2% merchant fee + 0.5% platform fee = 2.5% total
 * - DEPOSIT: 0€
 * - WITHDRAWAL: 1€ fixed
 */
@Entity
@Table(name = "transaction_fees", indexes = {
        @Index(name = "idx_fees_transaction", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFees {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "transaction_id")
    private TransactionCore transaction;

    /** Platform fee (commission for platform) */
    @Column(name = "platform_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    /** Merchant fee (commission charged to merchant) */
    @Column(name = "merchant_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal merchantFee = BigDecimal.ZERO;

    /** Total fees (sum of all fees) */
    @Column(name = "total_fees", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO;

    /** Fee calculation details (JSON string) */
    @Column(name = "fee_details", columnDefinition = "TEXT")
    private String feeDetails;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate total fees from platform and merchant fees
     */
    public void calculateTotalFees() {
        BigDecimal platform = platformFee != null ? platformFee : BigDecimal.ZERO;
        BigDecimal merchant = merchantFee != null ? merchantFee : BigDecimal.ZERO;
        this.totalFees = platform.add(merchant);
    }

    /**
     * Check if transaction has any fees
     */
    public boolean hasFees() {
        return totalFees != null && totalFees.compareTo(BigDecimal.ZERO) > 0;
    }
}
