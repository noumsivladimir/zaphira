package com.zaphira.transaction.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TransactionRefundedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long transactionId;
    private String transactionReference;
    private Long refundTransactionId;
    private String refundTransactionReference;
    private BigDecimal refundAmount;
    private BigDecimal refundFees;
    private BigDecimal totalRefundAmount;
    private String currency;
    @JsonProperty("refundType")
    private String refundType;
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
