package com.zaphira.transaction.model.enums;

/**
 * Enum representing the status of a dispute in the system.
 * 
 * Workflow: INITIATED → UNDER_INVESTIGATION → AWAITING_EVIDENCE → AWAITING_RESPONSE → RESOLVED → CLOSED
 * 
 * INITIATED: Dispute créé initialement, en attente de validation initiale
 * UNDER_INVESTIGATION: Équipe interne enquête sur le différend
 * AWAITING_EVIDENCE: En attente d'evidence supplémentaire du client
 * AWAITING_RESPONSE: En attente de réponse du défendant
 * RESOLVED: Décision de résolution prise
 * CLOSED: Cas fermé et montants finalisés
 * APPEAL_REQUESTED: Client a demandé un appel
 * ESCALATED: Cas escaladé à management/niveau supérieur
 * EXPIRED: Délai d'action dépassé sans résolution
 */
public enum DisputeStatus {
    
    /**
     * Dispute has been initiated and is awaiting initial validation
     */
    INITIATED("Dispute initiated"),
    
    /**
     * Under investigation by our team
     */
    UNDER_INVESTIGATION("Under investigation"),
    
    /**
     * Awaiting evidence submission from customer
     */
    AWAITING_EVIDENCE("Awaiting evidence"),
    
    /**
     * Awaiting response/counter-evidence from defendant
     */
    AWAITING_RESPONSE("Awaiting response"),
    
    /**
     * Resolution has been decided
     */
    RESOLVED("Resolved"),
    
    /**
     * Case is closed and amounts finalized
     */
    CLOSED("Closed"),
    
    /**
     * Customer has requested an appeal
     */
    APPEAL_REQUESTED("Appeal requested"),
    
    /**
     * Case has been escalated to management
     */
    ESCALATED("Escalated"),
    
    /**
     * Deadline for action has passed without resolution
     */
    EXPIRED("Expired");
    
    private final String description;
    
    DisputeStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
