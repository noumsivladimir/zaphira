package com.zaphira.service_user.dto.kyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for transaction limits based on KYC level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimitResponse {
    
    private Long userId;
    private String kycLevel; // NOT_SUBMITTED, PENDING, VERIFIED
    private Boolean isVerified;
    private Boolean canTransact;
    
    // Daily limits
    private BigDecimal dailyTransactionLimit;
    private BigDecimal dailySendLimit;
    private BigDecimal dailyReceiveLimit;
    
    // Monthly limits
    private BigDecimal monthlyTransactionLimit;
    private BigDecimal monthlySendLimit;
    private BigDecimal monthlyReceiveLimit;
    
    // Single transaction limits
    private BigDecimal maxSingleTransaction;
    private BigDecimal minSingleTransaction;
    
    // Other restrictions
    private Boolean canWithdraw;
    private Boolean canDeposit;
    private Boolean canTransferInternational;
    private Boolean requiresAdditionalVerification;
    
    private String message;
}
