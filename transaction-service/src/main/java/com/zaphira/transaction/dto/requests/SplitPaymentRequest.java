package com.zaphira.transaction.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for split payment transactions.
 * 
 * A split payment allows a single payment to be divided among multiple recipients.
 * Use cases:
 * - Sharing bills among friends
 * - Distributing commission/fees
 * - Multi-party settlements
 * 
 * Example:
 * Total: 10000 XOF
 * Recipients:
 *   - User A: 4000 XOF (40%)
 *   - User B: 3000 XOF (30%)
 *   - User C: 3000 XOF (30%)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SplitPaymentRequest {

    /**
     * Wallet number of the payer (sender).
     */
    @NotBlank(message = "Sender wallet number is required")
    private String senderWalletNumber;

    /**
     * Total amount to split (sum of all recipient shares must equal this).
     */
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal totalAmount;

    /**
     * Currency code (e.g., XOF, EUR).
     */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String currency;

    /**
     * List of recipients with their shares.
     * Must have at least 2 recipients.
     */
    @NotEmpty(message = "At least one recipient is required")
    @Size(min = 2, message = "Split payment requires at least 2 recipients")
    @Valid
    private List<RecipientShare> recipients;

    /**
     * Optional description for the payment.
     */
    private String description;

    /**
     * Optional reference number (e.g., bill ID, invoice number).
     */
    private String referenceNumber;

    /**
     * Nested DTO representing each recipient's share.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecipientShare {

        /**
         * Wallet number of the recipient.
         */
        @NotBlank(message = "Recipient wallet number is required")
        private String recipientWalletNumber;

        /**
         * Amount to transfer to this recipient.
         */
        @NotNull(message = "Amount is required for each recipient")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        /**
         * Optional label for this share (e.g., "Your share", "Commission").
         */
        private String label;
    }
}
