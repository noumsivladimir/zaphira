package com.zaphira.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {
    private Long id;
    private String walletNumber;
    
    // Balance fields (matching DB structure)
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal totalBalance;
    
    // Wallet info
    private String currency;
    private String type;
    private String status;
    private Long userId;
    
    // Limits and spending
    private BigDecimal dailyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlyLimit;
    private BigDecimal monthlySpent;
    
    // Frozen status
    private LocalDateTime frozenAt;
    private Long frozenBy;
    private String frozenReason;
    
    // Primary wallet flag
    private Boolean isPrimary;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime lastLimitReset;
    
    // Metadata and versioning
    private String metadata;
    private Long version;
    
    // Backward compatibility: balance property returns availableBalance
    public BigDecimal getBalance() {
        return this.availableBalance != null ? this.availableBalance : BigDecimal.ZERO;
    }
    
    public void setBalance(BigDecimal balance) {
        this.availableBalance = balance;
    }
    
    // Backward compatibility: active property based on frozenAt
    public Boolean getActive() {
        return this.frozenAt == null;
    }

    public String getStatus() {
        return this.status; 
    }
}

