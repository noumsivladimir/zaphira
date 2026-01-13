package com.zaphira.transaction.dto.response;

import com.zaphira.transaction.model.enums.CurrencyCode;
import lombok.*;

import java.math.BigDecimal;

/**
 * ExchangeRateResponse - FX rate information for API response.
 * 
 * Used by: GET /api/exchange-rates
 * 
 * Fields:
 * - sourceCurrency: From currency
 * - targetCurrency: To currency
 * - rate: Conversion rate
 * - bid/ask: Market prices (if available)
 * - provider: FX provider used
 * - expiresAt: Cache expiry time
 * 
 * Example:
 * {
 *   "sourceCurrency": "USD",
 *   "targetCurrency": "EUR",
 *   "rate": 0.92,
 *   "bid": 0.9195,
 *   "ask": 0.9205,
 *   "provider": "ECB",
 *   "expiresAt": "2025-12-17T15:00:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateResponse {
    private CurrencyCode sourceCurrency;
    private CurrencyCode targetCurrency;
    private BigDecimal rate;
    private BigDecimal bid;
    private BigDecimal ask;
    private String provider;
    private String expiresAt;
}
