package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO pour l'opération de reversal.
 * Contient les détails du reversal et l'état de la transaction après l'opération.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversalResponse {
    
    /**
     * ID de la transaction reversée
     */
    private Long originalTransactionId;
    
    /**
     * ID de la transaction compensatoire créée
     */
    private Long reversalTransactionId;
    
    /**
     * Référence de la transaction compensatoire
     */
    private String reversalReference;
    
    /**
     * Montant reversé
     */
    private BigDecimal reversalAmount;
    
    /**
     * Frais reversés (si applicables)
     */
    private BigDecimal reversalFees;
    
    /**
     * Devises
     */
    private String currency;
    
    /**
     * Statut de la transaction originale après reversal
     */
    private TransactionStatus originalTransactionStatus;
    
    /**
     * Statut de la transaction compensatoire
     */
    private TransactionStatus reversalTransactionStatus;
    
    /**
     * Raison du reversal
     */
    private String reason;
    
    /**
     * Moment du reversal
     */
    private LocalDateTime reversalTimestamp;
    
    /**
     * Utilisateur qui a effectué le reversal
     */
    private Long performedByUserId;
    
    /**
     * Rôle de l'utilisateur qui a effectué le reversal
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
