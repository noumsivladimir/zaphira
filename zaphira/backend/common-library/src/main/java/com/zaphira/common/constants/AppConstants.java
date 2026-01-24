package com.zaphira.common.constants;

/**
 * Application-wide constants
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public final class AppConstants {
    
    private AppConstants() {
        throw new UnsupportedOperationException("Constants class");
    }
    
    // ==================== GENERAL ====================
    public static final String APP_NAME = "Zaphira Platform";
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;
    
    // ==================== DATETIME ====================
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_ZONE_UTC = "UTC";
    
    // ==================== PAGINATION ====================
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    
    // ==================== VALIDATION ====================
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int PIN_LENGTH = 6;
    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRY_MINUTES = 10;
    
    // ==================== BUSINESS RULES ====================
    public static final double MIN_TRANSACTION_AMOUNT = 1.0;
    public static final double MAX_TRANSACTION_AMOUNT = 1000000.0;
    public static final double TRANSACTION_FEE_PERCENTAGE = 0.015; // 1.5%
    public static final double MIN_TRANSACTION_FEE = 1.0;
    
    // ==================== WALLET ====================
    public static final String DEFAULT_CURRENCY = "XAF";
    public static final double INITIAL_WALLET_BALANCE = 0.0;
    
    // ==================== KAFKA TOPICS ====================
    public static final String TOPIC_USER_EVENTS = "user-events";
    public static final String TOPIC_WALLET_EVENTS = "wallet-events";
    public static final String TOPIC_TRANSACTION_EVENTS = "transaction-events";
    public static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";
    public static final String TOPIC_VALIDATION_REQUESTS = "validation-requests";
    public static final String TOPIC_VALIDATION_RESULTS = "validation-results";
    
    // ==================== HEADERS ====================
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_API_KEY = "X-API-Key";
    
    // ==================== ROLES ====================
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_MERCHANT = "ROLE_MERCHANT";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    
    // ==================== ERROR CODES ====================
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_DUPLICATE = "DUPLICATE_RESOURCE";
    public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERROR_FORBIDDEN = "FORBIDDEN";
    public static final String ERROR_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String ERROR_TRANSACTION_FAILED = "TRANSACTION_FAILED";
    public static final String ERROR_INTERNAL = "INTERNAL_SERVER_ERROR";
    
    // ==================== CACHE NAMES ====================
    public static final String CACHE_USERS = "users";
    public static final String CACHE_WALLETS = "wallets";
    public static final String CACHE_EXCHANGE_RATES = "exchange-rates";
    public static final String CACHE_SETTINGS = "settings";
    
    // ==================== TTL (Time To Live) ====================
    public static final int JWT_EXPIRY_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int CACHE_TTL_MINUTES = 60;
}
