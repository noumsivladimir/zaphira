package com.zaphira.transaction.model.enums;

public enum TransactionStatus {
//    INITIATED,
//    PENDING,
//    AUTHORIZED,
//    PROCESSING,
//    COMPLETED,
//    FAILED,
//    CANCELLED,
//    REVERSED,
//    REFUNDED,
//    EXPIRED,
//    ON_HOLD,
//    UNDER_REVIEW


    PENDING("En attente", "Transaction initiée, en attente de traitement"),
    PROCESSING("En cours", "Transaction en cours de traitement"),
    AUTHORIZED ("Autorisée", "Transaction autorisée"),
    EXPIRED ("Expirée", "Transaction Expirée"),
    COMPLETED("Terminée", "Transaction complétée avec succès"),
    FAILED("Échouée", "Transaction échouée"),
    CANCELLED("Annulée", "Transaction annulée"),
    REVERSED("Inversée", "Transaction inversée/remboursée"),
    REFUNDED("Remboursée", "Transaction remboursée"),
    BLOCKED("Bloquée", "Transaction bloquée pour vérification");

    private final String label;
    private final String description;

    TransactionStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REVERSED;
    }

    public boolean canBeRetried() {
        return this == FAILED;
    }
}


