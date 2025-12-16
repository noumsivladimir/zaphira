package com.zaphira.transaction.service;

import com.zaphira.transaction.model.enums.CurrencyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * MultiCurrencyFeeService - Calculate FX fees and charges.
 * 
 * Responsibilities:
 * 1. Calculate FX conversion fees (percentage-based)
 * 2. Apply platform service fees
 * 3. Support fee tiers based on amount/currency
 * 4. Provide transparent fee breakdown
 * 
 * Fee Structure:
 * - Base FX Fee: 2.5% (configurable per currency pair)
 * - Service Fee: 0.5% (platform overhead)
 * - Total Cost to Customer: Base FX + Service
 * 
 * Example:
 * Transfer: USD 100 → EUR
 * FX Rate: 0.92 → EUR 92
 * FX Fee: 2.5% of 92 = EUR 2.30
 * Service Fee: 0.5% of 92 = EUR 0.46
 * Total Fees: EUR 2.76
 * Net Amount: EUR 88.94
 * 
 * Patterns (from Phase 2):
 * - Configurable via properties
 * - Big Decimal arithmetic for precision
 * - Detailed breakdown for transparency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiCurrencyFeeService {
    
    @Value("${fx.base-fee-percent:2.5}")
    private BigDecimal baseFeePercent;
    
    @Value("${fx.service-fee-percent:0.5}")
    private BigDecimal serviceFeePercent;
    
    // Per-currency-pair overrides (configurable)
    private static final Map<String, BigDecimal> CURRENCY_PAIR_FEES = new HashMap<>();
    
    static {
        // EUR pairs: lower fee (more liquid)
        CURRENCY_PAIR_FEES.put("USD_EUR", BigDecimal.valueOf(1.5));
        CURRENCY_PAIR_FEES.put("EUR_USD", BigDecimal.valueOf(1.5));
        CURRENCY_PAIR_FEES.put("EUR_GBP", BigDecimal.valueOf(1.5));
        
        // Emerging markets: higher fee (less liquid)
        CURRENCY_PAIR_FEES.put("USD_INR", BigDecimal.valueOf(3.5));
        CURRENCY_PAIR_FEES.put("USD_BRL", BigDecimal.valueOf(3.5));
        
        // JPY pairs: moderate fee
        CURRENCY_PAIR_FEES.put("USD_JPY", BigDecimal.valueOf(2.0));
    }
    
    // ============================================================
    // FEE CALCULATION
    // ============================================================
    
    /**
     * Calculate FX fee for currency conversion.
     * 
     * Fee is applied to converted amount (after rate conversion).
     * 
     * @param convertedAmount Amount after FX conversion
     * @param source Source currency
     * @param target Target currency
     * @return FX fee amount
     */
    public BigDecimal calculateFxFee(BigDecimal convertedAmount, CurrencyCode source, CurrencyCode target) {
        // Use pair-specific fee if exists, otherwise use base fee
        String pairKey = source.name() + "_" + target.name();
        BigDecimal feePercent = CURRENCY_PAIR_FEES.getOrDefault(pairKey, baseFeePercent);
        
        BigDecimal fee = convertedAmount
            .multiply(feePercent)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        log.debug(
            "[FEE_FX] Amount: {} {}, FeePercent: {}%, Fee: {}",
            convertedAmount, target, feePercent, fee
        );
        
        return fee;
    }
    
    /**
     * Calculate platform service fee.
     * 
     * Service fee is applied to final converted amount.
     * 
     * @param convertedAmount Amount after FX conversion
     * @param targetCurrency Target currency (for logging)
     * @return Service fee amount
     */
    public BigDecimal calculateServiceFee(BigDecimal convertedAmount, CurrencyCode targetCurrency) {
        BigDecimal fee = convertedAmount
            .multiply(serviceFeePercent)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        log.debug(
            "[FEE_SERVICE] Amount: {} {}, FeePercent: {}%, Fee: {}",
            convertedAmount, targetCurrency, serviceFeePercent, fee
        );
        
        return fee;
    }
    
    /**
     * Calculate total fees for a multi-currency transfer.
     * 
     * @param originalAmount Original amount in source currency
     * @param convertedAmount Amount after FX conversion to target currency
     * @param source Source currency
     * @param target Target currency
     * @return FeeBreakdown with FX fee, service fee, and total
     */
    public FeeBreakdown calculateTotalFees(
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        CurrencyCode source,
        CurrencyCode target
    ) {
        BigDecimal fxFee = calculateFxFee(convertedAmount, source, target);
        BigDecimal serviceFee = calculateServiceFee(convertedAmount, target);
        BigDecimal totalFees = fxFee.add(serviceFee);
        BigDecimal netAmount = convertedAmount.subtract(totalFees);
        
        FeeBreakdown breakdown = FeeBreakdown.builder()
            .originalAmount(originalAmount)
            .sourceCurrency(source)
            .convertedAmount(convertedAmount)
            .targetCurrency(target)
            .fxFee(fxFee)
            .serviceFee(serviceFee)
            .totalFees(totalFees)
            .netAmount(netAmount)
            .build();
        
        log.info(
            "[FEE_BREAKDOWN] Original: {} {}, Converted: {} {}, Fees: {} {}, Net: {} {}",
            originalAmount, source, convertedAmount, target,
            totalFees, target, netAmount, target
        );
        
        return breakdown;
    }
    
    /**
     * Calculate effective fee percentage on original amount.
     * 
     * Useful for transparency (e.g., "2.3% total cost").
     * 
     * @param breakdown Fee breakdown
     * @return Percentage of original amount (0-100)
     */
    public BigDecimal getEffectiveFeePercentage(FeeBreakdown breakdown) {
        return breakdown.getTotalFees()
            .divide(breakdown.getOriginalAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    // ============================================================
    // FEE BREAKDOWN DTO
    // ============================================================
    
    @lombok.Data
    @lombok.Builder
    public static class FeeBreakdown {
        private BigDecimal originalAmount;
        private CurrencyCode sourceCurrency;
        private BigDecimal convertedAmount;
        private CurrencyCode targetCurrency;
        private BigDecimal fxFee;
        private BigDecimal serviceFee;
        private BigDecimal totalFees;
        private BigDecimal netAmount;
    }
}
