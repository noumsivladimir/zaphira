package com.zaphira.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Response DTO
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    
    private Long id;
    private String transactionReference;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private Long userId;
    private Long accountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}