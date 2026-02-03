package com.zaphira.transaction.dto.response;

import com.zaphira.transaction.dto.TransactionDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for split payment operations.
 * 
 * Contains:
 * - Overall status of the split payment
 * - List of individual transactions created
 * - Summary statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SplitPaymentResponse {

    /**
     * Unique identifier for this split payment batch.
     */
    private String batchId;

    /**
     * Overall status: SUCCESS (all succeeded), PARTIAL (some failed), FAILED (all failed).
     */
    private SplitStatus status;

    /**
     * Total amount that was split.
     */
    private BigDecimal totalAmount;

    /**
     * Number of recipients.
     */
    private Integer totalRecipients;

    /**
     * Number of successful transactions.
     */
    private Integer successfulCount;

    /**
     * Number of failed transactions.
     */
    private Integer failedCount;

    /**
     * List of individual transactions created (one per recipient).
     */
    private List<TransactionDTO> transactions;

    /**
     * Optional error message if status is FAILED or PARTIAL.
     */
    private String errorMessage;

    /**
     * Enum for split payment status.
     */
    public enum SplitStatus {
        SUCCESS,    // All transactions succeeded
        PARTIAL,    // Some transactions failed
        FAILED      // All transactions failed
    }
}
