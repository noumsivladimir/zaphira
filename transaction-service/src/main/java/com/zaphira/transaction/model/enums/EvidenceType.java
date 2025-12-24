package com.zaphira.transaction.model.enums;

/**
 * Enum representing different types of evidence that can be submitted in a dispute.
 * 
 * Used to categorize and organize evidence submissions for dispute resolution.
 */
public enum EvidenceType {
    
    /**
     * Transaction receipt
     */
    RECEIPT("Receipt"),
    
    /**
     * Invoice from merchant
     */
    INVOICE("Invoice"),
    
    /**
     * Proof of delivery
     */
    DELIVERY_PROOF("Delivery proof"),
    
    /**
     * Communication records (email, chat, SMS)
     */
    COMMUNICATION("Communication"),
    
    /**
     * Contract or agreement
     */
    CONTRACT("Contract"),
    
    /**
     * Bank or financial statement
     */
    BANK_STATEMENT("Bank statement"),
    
    /**
     * Screenshot of transaction or communication
     */
    SCREENSHOT("Screenshot"),
    
    /**
     * Video recording evidence
     */
    VIDEO_RECORDING("Video recording"),
    
    /**
     * Audio recording evidence
     */
    AUDIO_RECORDING("Audio recording"),
    
    /**
     * Generic document
     */
    DOCUMENT("Document"),
    
    /**
     * Other type of evidence
     */
    OTHER("Other");
    
    private final String description;
    
    EvidenceType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
