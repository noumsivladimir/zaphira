package com.zaphira.transaction.model.enums;

/**
 * Enum representing different payment methods for transactions.
 * Each method has a priority and routing strategy.
 */
public enum PaymentMethod {
    // Direct payment methods
    WALLET(1, "wallet-internal"),
    BANK_TRANSFER(2, "bank-gateway"),
    CARD(3, "card-network"),
    
    // Digital payment methods
    MOBILE_MONEY(4, "mobile-money"),
    USSD(5, "ussd-gateway"),
    QR_CODE(6, "qr-payment-gateway"),
    
    // Alternative payment methods
    CRYPTO(7, "crypto-gateway"),
    CASH(8, "cash-handler"),
    CHECK(9, "check-processor"),
    
    // Aggregate methods
    AGENT(10, "agent-network"),
    ATM(11, "atm-network"),
    POS(12, "pos-network"),
    
    // Affiliate methods
    AFFILIATE(13, "affiliate-gateway"),
    REFERRAL(14, "referral-system");

    private final int priority;
    private final String routingKey;

    PaymentMethod(int priority, String routingKey) {
        this.priority = priority;
        this.routingKey = routingKey;
    }

    public int getPriority() {
        return priority;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * Get the next payment method in priority order (for fallback).
     * Returns null if this is the last method.
     */
    public PaymentMethod getNextFallback() {
        PaymentMethod[] methods = PaymentMethod.values();
        for (int i = 0; i < methods.length - 1; i++) {
            if (methods[i] == this) {
                return methods[i + 1];
            }
        }
        return null;
    }

    /**
     * Check if this is a direct payment method (wallet-to-wallet, bank, card).
     */
    public boolean isDirect() {
        return this == WALLET || this == BANK_TRANSFER || this == CARD;
    }

    /**
     * Check if this is a digital payment method.
     */
    public boolean isDigital() {
        return this == MOBILE_MONEY || this == USSD || this == QR_CODE;
    }

    /**
     * Check if this requires additional verification.
     */
    public boolean requiresVerification() {
        return this == CRYPTO || this == AGENT || this == CASH;
    }
}
