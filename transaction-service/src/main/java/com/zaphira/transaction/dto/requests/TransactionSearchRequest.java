package com.zaphira.transaction.dto.requests;

import com.zaphira.transaction.model.enums.TransactionCategory;
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

    private Long userId;

    private TransactionType type;

    private TransactionCategory category;

    private TransactionStatus status;

    private String senderWalletNumber; // Source ou destination

    private String receiverWalletNumber;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String transactionReference;

    private String externalReference;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    private Boolean isScheduled;
//
//    @Override
//    public String toString() {
//        return "TransactionSearchRequest{" +
//                "senderWalletNumber='" + walletNumber + '\'' +
//                ", receiverWalletNumber='" + receiverWalletNumber + '\'' +
//                ", amountMin=" + amountMin +
//                ", amountMax=" + amountMax +
//                ", status=" + status +
//                ", type=" + type +
//                ", createdFrom=" + createdFrom +
//                ", createdTo=" + createdTo +
//                ", reference='" + reference + '\'' +
//                ", currency='" + currency + '\'' +
//                ", route='" + route + '\'' +
//                ", scheduled=" + scheduled +
//                '}';
//    }
}
