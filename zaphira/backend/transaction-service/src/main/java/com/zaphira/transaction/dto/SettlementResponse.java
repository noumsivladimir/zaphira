package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.CurrencyCode;
import lombok.*;

import java.math.BigDecimal;

/**
 * SettlementResponse - Settlement details for API response.
 * 
 * Used by: GET /api/settlements/{id}
 * 
 * Fields:
 * - id: Settlement ID
 * - transactionId: Related transaction
 * - originalAmount: Initial amount
 * - settledAmount: After FX conversion
 * - netAmount: After fees
 * - status: PENDING, PROCESSING, COMPLETED, FAILED
 * - feeBreakdown: Detailed fees
 * - fxRate: Applied rate
 * 
 * Example:
 * {
 *   "id": 12345,
 *   "transactionId": "txn-001",
 *   "originalAmount": 100.00,
 *   "originalCurrency": "USD",
 *   "settledAmount": 92.00,
 *   "settlementCurrency": "EUR",
 *   "netAmount": 88.94,
 *   "status": "COMPLETED",
 *   "fxRate": 0.92,
 *   "totalFees": 3.06,
 *   "fxFee": 2.30,
 *   "serviceFee": 0.46,
 *   "settlementDate": "2025-12-16T10:30:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {
    private Long id;
    private Long transactionId;
    private BigDecimal originalAmount;
    private CurrencyCode originalCurrency;
    private BigDecimal settledAmount;
    private CurrencyCode settlementCurrency;
    private BigDecimal netAmount;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private BigDecimal fxRate;
    private String fxRateProvider;
    private BigDecimal totalFees;
    private BigDecimal fxFee;
    private BigDecimal serviceFee;
    private String settlementDate;
    private String failureReason;
    private Integer retryCount;
}
