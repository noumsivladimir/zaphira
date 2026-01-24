package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for advanced transaction search and filtering.
 * Supports complex queries with multiple filter criteria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSearchRequest {

    // Wallet filters
    private String senderWalletNumber;
    private String receiverWalletNumber;

    // Amount filters
    private BigDecimal amountMin;
    private BigDecimal amountMax;

    // Status & Type filters
    private TransactionStatus status;
    private TransactionType type;

    // Date range filters
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    // Reference & Currency filters
    private String reference;
    private String currency;

    // Additional filters
    private String route;
    private Boolean scheduled;

    @Override
    public String toString() {
        return "TransactionSearchRequest{" +
                "senderWalletNumber='" + senderWalletNumber + '\'' +
                ", receiverWalletNumber='" + receiverWalletNumber + '\'' +
                ", amountMin=" + amountMin +
                ", amountMax=" + amountMax +
                ", status=" + status +
                ", type=" + type +
                ", createdFrom=" + createdFrom +
                ", createdTo=" + createdTo +
                ", reference='" + reference + '\'' +
                ", currency='" + currency + '\'' +
                ", route='" + route + '\'' +
                ", scheduled=" + scheduled +
                '}';
    }
}
