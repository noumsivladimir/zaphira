package com.zaphira.common.model.enums;

public enum PermissionLevel {
    OWNER("Propriétaire - Tous les droits"),
    FULL_MANAGER("Gestionnaire Complet - Tous les droits sauf suppression"),
    TRANSACTION_MANAGER("Gestionnaire Transactions - Peut effectuer des transactions"),
    VIEWER("Visualiseur - Lecture seule"),
    LIMITED("Limité - Droits personnalisés");

    private final String description;

    PermissionLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}