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
public class TransactionDTO {
    private Long id;
    private String reference;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String type;
    private String channel;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

