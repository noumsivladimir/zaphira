package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.response.ExchangeRateResponse;
import com.zaphira.transaction.model.ExchangeRate;
import com.zaphira.transaction.model.enums.CurrencyCode;
import com.zaphira.transaction.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ExchangeRateController - FX rate lookups.
 * 
 * Endpoints:
 * - GET /api/exchange-rates - Get current rate
 * - GET /api/exchange-rates/refresh - Trigger cache refresh
 * 
 * Public endpoints (no auth required for rate lookups).
 * 
 * Pattern (from Phase 2 Controllers):
 * - JWT extraction not needed (public data)
 * - Exception handling via global @ControllerAdvice
 * - Proper HTTP status codes
 */
@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateController {
    
    private final ExchangeRateService exchangeRateService;
    
    // ============================================================
    // GET EXCHANGE RATE
    // ============================================================
    
    /**
     * Get current exchange rate between two currencies.
     * 
     * Example: GET /api/exchange-rates?from=USD&to=EUR
     * 
     * Response:
     * {
     *   "sourceCurrency": "USD",
     *   "targetCurrency": "EUR",
     *   "rate": 0.92,
     *   "bid": 0.9195,
     *   "ask": 0.9205,
     *   "provider": "ECB",
     *   "expiresAt": "2025-12-17T15:00:00"
     * }
     * 
     * @param from Source currency (ISO code)
     * @param to Target currency (ISO code)
     * @return Exchange rate with bid/ask
     */
    @GetMapping
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
        @RequestParam String from,
        @RequestParam String to
    ) {
        log.info("[FX_API] Request: {} → {}", from, to);
        
        try {
            CurrencyCode source = CurrencyCode.valueOf(from.toUpperCase());
            CurrencyCode target = CurrencyCode.valueOf(to.toUpperCase());
            
            ExchangeRate rate = exchangeRateService.getExchangeRate(source, target);
            
            ExchangeRateResponse response = ExchangeRateResponse.builder()
                .sourceCurrency(rate.getSourceCurrency())
                .targetCurrency(rate.getTargetCurrency())
                .rate(rate.getRate())
                .bid(rate.getBid())
                .ask(rate.getAsk())
                .provider(rate.getProvider())
                .expiresAt(rate.getExpiresAt().toString())
                .build();
            
            log.info("[FX_API_RESPONSE] Rate: {} (provider: {})", rate.getRate(), rate.getProvider());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[FX_API_ERROR] Invalid currency code: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Refresh exchange rate cache manually.
     * 
     * Admin endpoint for triggering cache refresh.
     * Normally runs on schedule every 12 hours.
     * 
     * @param from Source currency
     * @param to Target currency
     * @return OK if refreshed
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshRate(
        @RequestParam String from,
        @RequestParam String to
    ) {
        log.info("[FX_REFRESH_API] Manual refresh request: {} → {}", from, to);
        
        try {
            CurrencyCode source = CurrencyCode.valueOf(from.toUpperCase());
            CurrencyCode target = CurrencyCode.valueOf(to.toUpperCase());
            
            exchangeRateService.evictRate(source, target);
            // Next call to getExchangeRate will fetch fresh rate
            exchangeRateService.getExchangeRate(source, target);
            
            log.info("[FX_REFRESH_API_SUCCESS] Rate refreshed: {} → {}", from, to);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("[FX_REFRESH_API_ERROR] Invalid currency: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
