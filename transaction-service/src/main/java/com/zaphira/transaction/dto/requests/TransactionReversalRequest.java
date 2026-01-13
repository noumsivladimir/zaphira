package com.zaphira.transaction.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO pour l'opération de reversal de transaction.
 * Utilisé pour annuler une transaction complètement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversalRequest {
    
    /**
     * ID de la transaction à reverser
     */
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;
    
    /**
     * Raison du reversal
     * Examples: "Duplicate transaction", "Merchant request", "Fraud prevention"
     */
    @NotBlank(message = "Reason is required")
    private String reason;
    
    /**
     * Montant à reverser (généralement égal au montant de la transaction)
     */
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    /**
     * Inclure les frais dans le reversal
     */
    private Boolean includesFees;
    
    /**
     * Token pour step-up authentication (pour montants élevés)
     */
    private String sensitiveActionToken;
    
    /**
     * Remarques internes
     */
    private String internalNotes;
}
