package com.zaphira.common.utils;

import java.util.regex.Pattern;

/**
 * Utility class for validation operations
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public final class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    
    private static final Pattern PIN_PATTERN = Pattern.compile(
        "^\\d{4,6}$"
    );
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Validate email format
     * 
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number format (E.164)
     * 
     * @param phoneNumber Phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    /**
     * Validate PIN format (4-6 digits)
     * 
     * @param pin PIN to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPin(String pin) {
        return pin != null && PIN_PATTERN.matcher(pin).matches();
    }
    
    /**
     * Check if string is null or empty
     * 
     * @param str String to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Check if string is not null and not empty
     * 
     * @param str String to check
     * @return true if not empty, false otherwise
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Validate amount (must be positive)
     * 
     * @param amount Amount to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidAmount(Double amount) {
        return amount != null && amount > 0;
    }
    
    /**
     * Validate currency code (ISO 4217)
     * 
     * @param currencyCode Currency code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCurrencyCode(String currencyCode) {
        return currencyCode != null && 
               currencyCode.length() == 3 && 
               currencyCode.matches("[A-Z]{3}");
    }
}
