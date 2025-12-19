package com.zaphira.wallet.models.enums;

public enum WalletType {
    USER("Utilisateur classique"),
    MERCHANT("Wallet Marchant"),
    AGENT("Wallet De Terrain"),
    SYSTEM("Wallet Technique Interne"), // Ne dois pas etre exposé par API
    BUSINESS("Wallet entreprise");

    private final String description;

    WalletType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}