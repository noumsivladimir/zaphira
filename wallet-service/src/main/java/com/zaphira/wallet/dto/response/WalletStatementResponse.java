package com.zaphira.wallet.dto.response;

import com.zaphira.common.dto.TransactionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for wallet statement/account summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatementResponse {
    private String walletNumber;
    private String ownerName;
    private String currency;
    
    // Balance information
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    
    // Period information
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime generatedAt;
    
    // Transaction details
    private List<TransactionDTO> transactions;
    private int transactionCount;
    
    // Additional info
    private String statementId;
}
