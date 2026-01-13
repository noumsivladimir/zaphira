package com.zaphira.transaction.exception;

import java.math.BigDecimal;

/**
 * Classe conteneur pour toutes les exceptions du module Transaction
 */
public class TransactionExceptions {

    // ✅ Empêcher l'instanciation de la classe conteneur
    private TransactionExceptions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========== Transaction Exceptions ==========

    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }

        public TransactionNotFoundException(Long transactionId) {
            super("Transaction with ID " + transactionId + " not found");
        }
    }

    public static class InvalidTransactionStatusException extends RuntimeException {
        public InvalidTransactionStatusException(String message) {
            super(message);
        }
    }

    public static class InvalidTransactionRequestException extends RuntimeException {
        public InvalidTransactionRequestException(String message) {
            super(message);
        }
    }

    public static class DuplicateTransactionException extends RuntimeException {
//        public DuplicateTransactionException(String message) {
//            super(message);
//        }

        public DuplicateTransactionException(String reference) {
            super("Transaction with reference " + reference + " already exists");
        }
    }

    // ========== Wallet Related Exceptions ==========

    public static class WalletServiceException extends RuntimeException {
        public WalletServiceException(String message) {
            super(message);
        }

        public WalletServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class WalletValidationException extends RuntimeException {
        public WalletValidationException(String message) {
            super(message);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }

        public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
            super(String.format("Insufficient balance. Required: %s, Available: %s", required, available));
        }
    }
}