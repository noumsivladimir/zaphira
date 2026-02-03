package com.zaphira.transaction.model.enums;

public enum TransactionType {
//    P2P_TRANSFER,
//    INTERNAL_TRANSFER,
//    CROSS_BORDER_TRANSFER,
//    BULK_TRANSFER,
//    SPLIT_TRANSFER,
//    GROUP_TRANSFER,
//    MERCHANT_PAYMENT,
//    BILL_PAYMENT,
//    SUBSCRIPTION_PAYMENT,
//    INVOICE_PAYMENT,
//    QR_PAYMENT,
//    PAYMENT_LINK,
//    IN_APP_PAYMENT,
//    CONTACTLESS_PAYMENT,
//    MOBILE_RECHARGE,
//    WALLET_TOPUP,
//    TRANSIT_TOPUP,
//    GIFT_CARD_PURCHASE,
//    ATM_WITHDRAWAL,
//    AGENT_WITHDRAWAL,
//    BANK_TRANSFER,
//    CARD_TRANSFER,
//    CRYPTO_WITHDRAWAL,
//    ESCROW,
//    SCHEDULED,
//    RECURRING,
//    SPLIT_BILL,
//    REQUEST_MONEY,
//    DONATION,
//    TIP,

    DEBIT("Débit", "Retrait du compte"),
    CREDIT("Crédit", "Ajout au compte"),
    TRANSFER("Transfert", "Transfert entre comptes"),
    WITHDRAWAL("Retrait", "Retrait d'argent"),
    DEPOSIT("Dépôt", "Dépôt d'argent"),
    MERCHANT_PAYMENT("Paiement Marchand", "Paiement chez un commerçant"),
    REFUND("Remboursement", "Remboursement d'une transaction"),
    FEE("Frais", "Frais de service"),
    PAYMENT("Paiement", "Paiement d'un service"),
    REVERSAL("Annulation", "Annulation de transaction");


    private final String label;
    private final String description;

    TransactionType(String label, String description) {
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


