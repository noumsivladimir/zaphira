package com.zaphira.transaction.dto.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionCoreDTO - DTO for LOT 1 + LOT 2
 * 
 * LOT 1: Basic fields
 * LOT 2: Added fees, metadata fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCoreDTO {

    private Long id;

    private String reference;

    private Long senderWalletId;

    private Long receiverWalletId;

    private BigDecimal amount;

    private String currency;

    private TransactionType type;

    private TransactionStatus status;

    private String description;

    // LOT 2: Fee fields
    private BigDecimal feeAmount;

    private BigDecimal totalAmount;

    private BigDecimal platformFee;

    private BigDecimal merchantFee;

    // LOT 2: Metadata fields
    private String failureReason;

    private Integer retryCount;

    private Integer maxRetryAttempts;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
}
