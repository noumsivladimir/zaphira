package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zaphira.transaction.model.enums.TransactionCategory;
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
public class TransactionDTO {

    private Long id;

    private Long userId;

    //    private String sourceAccountNumber;
    private String senderWalletNumber;
    private Long senderWalletId;

//    private String destinationAccountNumber;
    private String receiverWalletNumber;
    private Long receiverWalletId;

    private Long subWalletId;


    private BigDecimal amount;

    private String currency;

    private String reference;

    private TransactionType type;

    private TransactionCategory category;

    private TransactionStatus status;



    private BigDecimal feeAmount;

    private BigDecimal totalAmount;

    private String description;

    private String externalReference;

    private Long relatedTransactionId;

    private String failureReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    private String metadata;
}