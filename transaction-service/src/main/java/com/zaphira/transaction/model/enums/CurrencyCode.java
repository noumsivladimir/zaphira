package com.zaphira.transaction.model.enums;

/**
 * CurrencyCode - ISO 4217 currency codes.
 * 
 * Supported currencies for multi-currency transactions.
 * Pattern: 3-letter ISO codes (USD, EUR, GBP, etc.)
 */
public enum CurrencyCode {
    USD("United States Dollar"),
    EUR("Euro"),
    GBP("British Pound Sterling"),
    JPY("Japanese Yen"),
    CHF("Swiss Franc"),
    CAD("Canadian Dollar"),
    AUD("Australian Dollar"),
    NZD("New Zealand Dollar"),
    CNY("Chinese Yuan"),
    INR("Indian Rupee"),
    MXN("Mexican Peso"),
    BRL("Brazilian Real"),
    ZAR("South African Rand"),
    SGD("Singapore Dollar"),
    HKD("Hong Kong Dollar"),
    KRW("South Korean Won"),
    AED("United Arab Emirates Dirham"),
    SAR("Saudi Arabian Riyal");
    
    private final String description;
    
    CurrencyCode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
