package com.zaphira.transaction.model.enums;

public enum TransactionCategory {
    WALLET_TO_WALLET("Wallet à Wallet", "Transfert entre wallets"),
    MOBILE_MONEY("Mobile Money", "Transaction via opérateur mobile"),
//    BANK_TRANSFER("Virement bancaire", "Transfert bancaire"),
    CASH("Espèces", "Transaction en espèces"),
//    BILL_PAYMENT("Paiement de facture", "Paiement de services"),
    MERCHANT_PAYMENT("Paiement marchand", "Paiement chez un commerçant"),
//    AIRTIME("Recharge", "Recharge de crédit téléphonique"),
    INTERNAL("Interne", "Transaction interne système");

    private final String label;
    private final String description;

    TransactionCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
