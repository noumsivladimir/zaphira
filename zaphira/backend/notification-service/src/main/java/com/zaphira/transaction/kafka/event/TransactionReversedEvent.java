package com.zaphira.transaction.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long transactionId;
    private String transactionReference;
    private Long reversalTransactionId;
    private String reversalTransactionReference;
    private BigDecimal amount;
    private BigDecimal fees;
    private String currency;
    private Long senderUserId;
    private String senderWalletNumber;
    private Long receiverUserId;
    private String receiverWalletNumber;
    private String reason;
    private Long performedByUserId;
    private String performedByRole;
    private LocalDateTime timestamp;
    private String requestId;
}
