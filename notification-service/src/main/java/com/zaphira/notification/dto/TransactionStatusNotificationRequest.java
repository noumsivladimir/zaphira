package com.zaphira.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for transaction status change notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusNotificationRequest {
    
    private Long transactionId;
    private String transactionReference;
    private String previousStatus;
    private String newStatus;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private String reason; // Optional reason for status change
}
