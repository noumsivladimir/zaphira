package com.zaphira.transaction.event;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SettlementCompletedEvent - Settlement completion event.
 * 
 * Triggered: When settlement transaction is marked COMPLETED
 * 
 * Consumed by:
 * - Wallet Service: Update merchant wallet with net amount
 * - Notification Service: Email to merchant
 * - Analytics Service: Settlement reporting
 * - Accounting Service: Financial records
 * 
 * Fields:
 * - settlementId: Record ID
 * - transactionId: Original transaction
 * - originalAmount: Initial amount
 * - settledAmount: Amount after FX conversion
 * - netAmount: Amount after fee deduction
 * - totalFees: Sum of all fees
 * - fxRate: Applied exchange rate
 * 
 * Pattern (from Phase 2):
 * - Transactional payload
 * - Kafka topic: settlement-completed
 * - Key: transactionId (for partitioning)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementCompletedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long settlementId;
    private String transactionId; // String for event interop
    private BigDecimal originalAmount;
    private String originalCurrency;
    private BigDecimal settledAmount;
    private String settlementCurrency;
    private BigDecimal fxRate;
    private BigDecimal totalFees;
    private BigDecimal netAmount;
    private LocalDateTime settlementDate;
    private String processedBy; // User email or "SYSTEM"
}
