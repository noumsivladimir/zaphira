package com.zaphira.transaction.event;

import com.zaphira.common.model.enums.Currency;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
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
public class TransactionEvent {

    private String eventType; // CREATED, COMPLETED, FAILED, CANCELLED

    private String transactionReference;

//    private Long userId;

    private TransactionType type;

    private TransactionStatus status;

    private Long senderWalletId;

    private String sourceAccountNumber;

    private Long receiverWalletId;

    private String destinationAccountNumber;

    private BigDecimal amount;

    private Currency currency;

    private BigDecimal feeAmount;

    private BigDecimal totalAmount;

    private String failureReason;

    private LocalDateTime timestamp;
}