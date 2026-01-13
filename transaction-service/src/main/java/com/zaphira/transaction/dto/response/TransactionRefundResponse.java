package com.zaphira.transaction.dto.response;

import com.zaphira.transaction.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO pour l'opération de remboursement.
 * Contient les détails du remboursement et l'état de la transaction après l'opération.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRefundResponse {
    
    /**
     * ID de la transaction remboursée
     */
    private Long originalTransactionId;
    
    /**
     * ID de la transaction de remboursement créée
     */
    private Long refundTransactionId;
    
    /**
     * Référence de la transaction de remboursement
     */
    private String refundReference;
    
    /**
     * Montant remboursé au client
     */
    private BigDecimal refundAmount;
    
    /**
     * Frais remboursés (si applicables)
     */
    private BigDecimal refundFees;
    
    /**
     * Montant total remboursé (montant + frais)
     */
    private BigDecimal totalRefundAmount;
    
    /**
     * Montant restant à rembourser (pour remboursements partiels)
     */
    private BigDecimal remainingRefundable;
    
    /**
     * Devises
     */
    private String currency;
    
    /**
     * Type de remboursement: FULL ou PARTIAL
     */
    private String refundType;
    
    /**
     * Statut de la transaction originale après remboursement
     */
    private TransactionStatus originalTransactionStatus;
    
    /**
     * Statut de la transaction de remboursement
     */
    private TransactionStatus refundTransactionStatus;
    
    /**
     * Raison du remboursement
     */
    private String reason;
    
    /**
     * Moment du remboursement
     */
    private LocalDateTime refundTimestamp;
    
    /**
     * Utilisateur qui a effectué le remboursement
     */
    private Long performedByUserId;
    
    /**
     * Rôle de l'utilisateur qui a effectué le remboursement
     */
    private String performedByRole;
    
    /**
     * Message de succès
     */
    private String message;
    
    /**
     * Code de succès
     */
    private String code;
}
