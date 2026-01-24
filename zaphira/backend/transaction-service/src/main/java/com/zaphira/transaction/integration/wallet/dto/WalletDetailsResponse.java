package com.zaphira.transaction.integration.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDetailsResponse {
    private Long id;
    private String walletNumber;
    private Long userId;
    
    // Balance fields (matching Wallet entity structure)
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal totalBalance;
    
    // Wallet info
    private String currency;
    private String type;
    private String status;
    private boolean frozen;
    
    // Limits
    private BigDecimal dailyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlyLimit;
    private BigDecimal monthlySpent;
    
    // Frozen details
    private LocalDateTime frozenAt;
    private Long frozenBy;
    private String frozenReason;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Backward compatibility: balance property returns availableBalance
    public BigDecimal getBalance() {
        return this.availableBalance != null ? this.availableBalance : BigDecimal.ZERO;
    }
    
    public void setBalance(BigDecimal balance) {
        this.availableBalance = balance;
    }
}


