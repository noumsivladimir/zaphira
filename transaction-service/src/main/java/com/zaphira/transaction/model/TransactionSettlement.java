package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.CurrencyCode;
import com.zaphira.transaction.model.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionSettlement - Record of settlement with FX conversion.
 * 
 * Purpose:
 * - Track final settlement amounts after FX conversion
 * - Record fees applied (FX fee, service fee, etc.)
 * - Audit trail for settlement process
 * - Support multi-currency transfers
 * 
 * Example:
 * transaction: USD 100 → EUR
 * originalAmount: 100 USD
 * settledAmount: 92 EUR (after FX conversion)
 * fxRate: 0.92
 * fxFee: 2.5 EUR (2.5% fee)
 * 
 * Pattern (from Phase 2):
 * - One-to-One with Transaction
 * - Settlement timeline tracking
 * - Fee breakdown for transparency
 */
@Entity
@Table(name = "transaction_settlements", indexes = {
    @Index(name = "idx_settlements_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_settlements_status", columnList = "status"),
    @Index(name = "idx_settlements_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSettlement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SettlementStatus status;
    
    // Original transaction amount (in original currency)
    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;
    
    @Column(name = "original_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyCode originalCurrency;
    
    // Settled amount (in settlement currency - may differ if FX conversion)
    @Column(name = "settled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount;
    
    @Column(name = "settlement_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyCode settlementCurrency;
    
    // FX conversion details
    @Column(name = "fx_rate", precision = 19, scale = 8)
    private BigDecimal fxRate;
    
    @Column(name = "fx_rate_provider")
    private String fxRateProvider; // "ECB", "OPENEXCHANGERATES", etc.
    
    // Fee breakdown
    @Column(name = "fx_fee", precision = 19, scale = 2)
    private BigDecimal fxFee; // Currency conversion fee
    
    @Column(name = "service_fee", precision = 19, scale = 2)
    private BigDecimal serviceFee; // Platform service fee
    
    @Column(name = "total_fees", precision = 19, scale = 2)
    private BigDecimal totalFees;
    
    // Net amount after all deductions
    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;
    
    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "failure_reason")
    private String failureReason; // If status = FAILED
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Calculate net amount after all fees.
     * 
     * @return net amount
     */
    public BigDecimal calculateNetAmount() {
        return this.settledAmount
            .subtract(this.fxFee != null ? this.fxFee : BigDecimal.ZERO)
            .subtract(this.serviceFee != null ? this.serviceFee : BigDecimal.ZERO);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (this.netAmount == null) {
            this.netAmount = calculateNetAmount();
        }
    }
}
