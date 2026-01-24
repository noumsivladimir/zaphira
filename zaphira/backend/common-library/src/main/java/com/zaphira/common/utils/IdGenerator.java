package com.zaphira.common.utils;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility class for generating unique identifiers and codes
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public final class IdGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Generate a unique UUID string
     * 
     * @return UUID string
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generate a user ID with prefix
     * Example: USR_550e8400-e29b-41d4-a716-446655440000
     * 
     * @return User ID
     */
    public static String generateUserId() {
        return "USR_" + UUID.randomUUID();
    }
    
    /**
     * Generate a wallet ID with prefix
     * Example: WLT_550e8400-e29b-41d4-a716-446655440000
     * 
     * @return Wallet ID
     */
    public static String generateWalletId() {
        return "WLT_" + UUID.randomUUID();
    }
    
    /**
     * Generate a transaction reference number
     * Format: TXN{YYYYMMDD}{6-digit random}
     * Example: TXN20260121123456
     * 
     * @return Transaction reference
     */
    public static String generateTransactionReference() {
        String date = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE
        );
        String random = String.format("%06d", RANDOM.nextInt(1000000));
        return "TXN" + date + random;
    }
    
    /**
     * Generate a settlement batch ID
     * Format: STL{YYYYMMDD}{6-digit random}
     * Example: STL20260121123456
     * 
     * @return Settlement batch ID
     */
    public static String generateSettlementBatchId() {
        String date = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE
        );
        String random = String.format("%06d", RANDOM.nextInt(1000000));
        return "STL" + date + random;
    }
    
    /**
     * Generate a dispute ID
     * Format: DSP{YYYYMMDD}{6-digit random}
     * Example: DSP20260121123456
     * 
     * @return Dispute ID
     */
    public static String generateDisputeId() {
        String date = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.BASIC_ISO_DATE
        );
        String random = String.format("%06d", RANDOM.nextInt(1000000));
        return "DSP" + date + random;
    }
    
    /**
     * Generate a numeric OTP code
     * 
     * @param length Length of OTP (4-8 digits)
     * @return OTP code
     */
    public static String generateOTP(int length) {
        if (length < 4 || length > 8) {
            throw new IllegalArgumentException("OTP length must be between 4 and 8");
        }
        int max = (int) Math.pow(10, length) - 1;
        int min = (int) Math.pow(10, length - 1);
        int otp = RANDOM.nextInt(max - min + 1) + min;
        return String.format("%0" + length + "d", otp);
    }
    
    /**
     * Generate a 6-digit OTP code
     * 
     * @return 6-digit OTP
     */
    public static String generateOTP() {
        return generateOTP(6);
    }
    
    /**
     * Generate an alphanumeric code
     * 
     * @param length Length of the code
     * @return Alphanumeric code
     */
    public static String generateAlphanumericCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length())));
        }
        return code.toString();
    }
    
    /**
     * Generate a referral code
     * 
     * @return 8-character alphanumeric referral code
     */
    public static String generateReferralCode() {
        return generateAlphanumericCode(8);
    }
}
