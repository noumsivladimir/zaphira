package com.zaphira.transaction.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO pour l'opération de remboursement (refund) de transaction.
 * Supporte les remboursements partiels et complets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRefundRequest {
    
    /**
     * ID de la transaction à rembourser
     */
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;
    
    /**
     * Montant à rembourser (peut être inférieur au montant original pour remboursement partiel)
     */
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;
    
    /**
     * Raison du remboursement
     * Examples: "Defective goods", "Customer request", "Cancellation"
     */
    @NotBlank(message = "Reason is required")
    private String reason;
    
    /**
     * Doit-on rembourser les frais?
     */
    private Boolean includeFees;
    
    /**
     * Montant des frais à rembourser (si applicables)
     */
    @DecimalMin(value = "0.00", message = "Fee amount cannot be negative")
    private BigDecimal refundFeeAmount;
    
    /**
     * Token pour step-up authentication (pour montants élevés)
     */
    private String sensitiveActionToken;
    
    /**
     * Remarques internes
     */
    private String internalNotes;
    
    /**
     * Type de remboursement: FULL ou PARTIAL
     */
    private RefundType refundType;
    
    /**
     * Référence de politique de remboursement
     */
    private String refundPolicyRef;
    
    public enum RefundType {
        FULL,      // Remboursement complet
        PARTIAL    // Remboursement partiel
    }
}
