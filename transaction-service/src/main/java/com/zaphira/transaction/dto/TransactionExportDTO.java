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
 * DTO for transaction export in CSV/JSON formats.
 * Contains essential transaction information for reporting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionExportDTO {

    private Long id;
    private String reference;
    private String senderWalletNumber;
    private String receiverWalletNumber;
    private BigDecimal amount;
    private String currency;
    private BigDecimal feeAmount;
    private String feeCurrency;
    private TransactionType type;
    private TransactionStatus status;
    private String route;
    private String description;
    private Integer riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime cancelledAt;

    @Override
    public String toString() {
        return reference + "," + senderWalletNumber + "," + receiverWalletNumber + "," +
                amount + "," + currency + "," + status + "," + createdAt;
    }
}
