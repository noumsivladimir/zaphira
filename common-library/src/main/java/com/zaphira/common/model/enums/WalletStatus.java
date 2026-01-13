package com.zaphira.common.model.enums;

public enum WalletStatus {
    ACTIVE("Actif - Opérationnel"),
    FROZEN("Gelé - Temporairement bloqué"),
    SUSPENDED("Suspendu - En attente de vérification"),
    CLOSED("Fermé - Définitivement fermé");

    private final String description;

    WalletStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransact() {
        return this == ACTIVE;
    }

    public boolean canReceive() {
        return this == ACTIVE || this == FROZEN;
    }
}