package com.zaphira.transaction.dto.response;

import com.zaphira.transaction.model.enums.ScheduledTransactionStatus;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ScheduledTransactionResponse {

    private Long id;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionChannel channel;
    private String description;
    private String requestedBy;
    private LocalDateTime scheduledFor;
    private ScheduledTransactionStatus status;
    private String lastError;
    private Long executedTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecutionAt;
}


