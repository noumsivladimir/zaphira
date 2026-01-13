package com.zaphira.transaction.dto.requests;

import com.zaphira.transaction.model.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    /* =========================================================
       CORE TRANSACTION
       ========================================================= */

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Transaction category is required")
    private TransactionCategory category;

    @NotNull(message = "Transaction channel is required")
    private TransactionChannel channel;

    @Positive(message = "Amount must be positive")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    /* =========================================================
       WALLETS
       ========================================================= */

    // Required for DEBIT, TRANSFER, WITHDRAWAL
    @NotNull
    @NotBlank
    private String senderWalletNumber;

    // Required for CREDIT, TRANSFER, DEPOSIT
    @NotNull
    @NotBlank
    private String receiverWalletNumber;

    /* =========================================================
       PAYMENT / ROUTING
       ========================================================= */

    private PaymentMethod paymentMethod;      // WALLET, CARD, BANK, USSD…
    private String route;                      // WALLET_INTERNAL, MTN_MOMO, ORANGE_MONEY…
    private String transactionReference;       // External / partner reference (optional)

    /* =========================================================
       FEES (OPTIONAL – can be computed server-side)
       ========================================================= */

    private BigDecimal feeAmount;
    private String feeType;                    // FIXED / PERCENTAGE / TIERED

    /* =========================================================
       AUTHORIZATION
       ========================================================= */

    private Boolean authorizationRequired;
    private AuthorizationMethod authorizationMethod; // PIN, OTP, BIOMETRIC

    /* =========================================================
       SCHEDULING
       ========================================================= */

    private Boolean scheduled;
    private String scheduledFor; // ISO-8601 string (parsed côté service)

    /* =========================================================
       CONTEXT / TRACEABILITY
       ========================================================= */

    private String description;
    private String initiatedBy;                // USER / SYSTEM / ADMIN / API
    private String ipAddress;
    private String deviceInfo;

    /* =========================================================
       COMPLIANCE / META
       ========================================================= */

    private String metadata;                   // JSON string (risk, geo, tags…)
}
