package com.zaphira.transaction.service;

import com.zaphira.transaction.model.core.TransactionCore;
import com.zaphira.transaction.model.core.TransactionFees;
import com.zaphira.transaction.model.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FeeCalculationService - LOT 2
 * 
 * Centralized fee calculation logic:
 * - TRANSFER: 0.5% platform fee
 * - MERCHANT_PAYMENT: 2% merchant fee + 0.5% platform fee = 2.5% total
 * - DEPOSIT: 0€
 * - WITHDRAWAL: 1€ fixed
 * - PAYMENT: 0.5% platform fee
 * - REFUND: 0€
 * - REVERSAL: 0€
 */
@Slf4j
@Service
public class FeeCalculationService {

    // Fee rates as constants
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal MERCHANT_FEE_RATE = new BigDecimal("0.02");  // 2%
    private static final BigDecimal WITHDRAWAL_FIXED_FEE = new BigDecimal("1.00");
    private static final int SCALE = 4;

    /**
     * Calculate fees for a transaction
     */
    public TransactionFees calculateFees(TransactionCore transaction) {
        log.debug("Calculating fees for transaction type: {}", transaction.getType());

        TransactionFees fees = TransactionFees.builder()
                .transaction(transaction)
                .platformFee(BigDecimal.ZERO)
                .merchantFee(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .build();

        TransactionType type = transaction.getType();
        BigDecimal amount = transaction.getAmount();

        switch (type) {
            case TRANSFER:
                fees.setPlatformFee(calculatePercentage(amount, PLATFORM_FEE_RATE));
                break;

            case MERCHANT_PAYMENT:
                fees.setMerchantFee(calculatePercentage(amount, MERCHANT_FEE_RATE));
                fees.setPlatformFee(calculatePercentage(amount, PLATFORM_FEE_RATE));
                break;

            case PAYMENT:
                fees.setPlatformFee(calculatePercentage(amount, PLATFORM_FEE_RATE));
                break;

            case WITHDRAWAL:
                fees.setPlatformFee(WITHDRAWAL_FIXED_FEE);
                break;

            case DEPOSIT:
            case REFUND:
            case REVERSAL:
                // No fees
                break;

            default:
                log.warn("Unknown transaction type for fee calculation: {}", type);
        }

        fees.calculateTotalFees();
        
        // Create fee details JSON
        fees.setFeeDetails(createFeeDetails(fees, type));

        log.debug("Calculated fees - Platform: {}, Merchant: {}, Total: {}",
                fees.getPlatformFee(), fees.getMerchantFee(), fees.getTotalFees());

        return fees;
    }

    /**
     * Calculate percentage-based fee
     */
    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal rate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Create fee details JSON string
     */
    private String createFeeDetails(TransactionFees fees, TransactionType type) {
        return String.format(
                "{\"type\":\"%s\",\"platformFee\":%s,\"merchantFee\":%s,\"totalFees\":%s,\"platformRate\":\"0.5%%\",\"merchantRate\":\"2%%\"}",
                type,
                fees.getPlatformFee(),
                fees.getMerchantFee(),
                fees.getTotalFees()
        );
    }

    /**
     * Calculate total amount including fees
     */
    public BigDecimal calculateTotalAmount(BigDecimal amount, BigDecimal feeAmount) {
        BigDecimal amt = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal fee = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        return amt.add(fee);
    }

    /**
     * Get platform fee rate
     */
    public BigDecimal getPlatformFeeRate() {
        return PLATFORM_FEE_RATE;
    }

    /**
     * Get merchant fee rate
     */
    public BigDecimal getMerchantFeeRate() {
        return MERCHANT_FEE_RATE;
    }

    /**
     * Get withdrawal fixed fee
     */
    public BigDecimal getWithdrawalFixedFee() {
        return WITHDRAWAL_FIXED_FEE;
    }

    /**
     * Check if transaction type has fees
     */
    public boolean hasFees(TransactionType type) {
        return type == TransactionType.TRANSFER ||
               type == TransactionType.MERCHANT_PAYMENT ||
               type == TransactionType.PAYMENT ||
               type == TransactionType.WITHDRAWAL;
    }
}
