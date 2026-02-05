package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.request.CurrencyConversionRequest;
import com.zaphira.transaction.dto.response.CurrencyConversionResponse;
import com.zaphira.transaction.dto.response.ExchangeRateHistoryDTO;
import com.zaphira.transaction.dto.response.ExchangeRateResponse;
import com.zaphira.transaction.model.ExchangeRate;
import com.zaphira.transaction.model.enums.CurrencyCode;
import com.zaphira.transaction.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    
    /**
     * Convert amount between currencies.
     * 
     * POST /api/exchange-rates/convert
     * 
     * Request Body:
     * {
     *   "amount": 100.00,
     *   "from": "USD",
     *   "to": "EUR"
     * }
     * 
     * Response:
     * {
     *   "sourceAmount": 100.00,
     *   "sourceCurrency": "USD",
     *   "convertedAmount": 92.00,
     *   "targetCurrency": "EUR",
     *   "exchangeRate": 0.92,
     *   "provider": "ECB"
     * }
     * 
     * @param request Conversion request with amount, from and to currencies
     * @return Conversion result with converted amount
     */
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request) {
        
        log.info("[FX_CONVERT_API] Converting {} {} → {}", 
                request.getAmount(), request.getFrom(), request.getTo());
        
        try {
            CurrencyCode source = CurrencyCode.valueOf(request.getFrom().toUpperCase());
            CurrencyCode target = CurrencyCode.valueOf(request.getTo().toUpperCase());
            
            // Get exchange rate
            ExchangeRate rate = exchangeRateService.getExchangeRate(source, target);
            
            // Convert amount
            BigDecimal convertedAmount = exchangeRateService.convert(
                    request.getAmount(), source, target);
            
            CurrencyConversionResponse response = CurrencyConversionResponse.builder()
                    .sourceAmount(request.getAmount())
                    .sourceCurrency(request.getFrom().toUpperCase())
                    .convertedAmount(convertedAmount)
                    .targetCurrency(request.getTo().toUpperCase())
                    .exchangeRate(rate.getRate())
                    .provider(rate.getProvider())
                    .build();
            
            log.info("[FX_CONVERT_API_SUCCESS] {} {} = {} {} (rate: {})", 
                    request.getAmount(), source, convertedAmount, target, rate.getRate());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[FX_CONVERT_API_ERROR] Invalid currency: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get exchange rate history for a currency pair.
     * 
     * GET /api/exchange-rates/history?from=USD&to=EUR&limit=100
     * 
     * Query params:
     * - from: Source currency (ISO code)
     * - to: Target currency (ISO code)
     * - limit: Maximum number of records (default: 100, max: 500)
     * 
     * Response: List of historical rates ordered by date (newest first)
     * 
     * @param from Source currency
     * @param to Target currency
     * @param limit Maximum records to return
     * @return List of historical exchange rates
     */
    @GetMapping("/history")
    public ResponseEntity<List<ExchangeRateHistoryDTO>> getExchangeRateHistory(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "100") int limit) {
        
        log.info("[FX_HISTORY_API] Fetching history: {} → {} (limit: {})", from, to, limit);
        
        try {
            CurrencyCode source = CurrencyCode.valueOf(from.toUpperCase());
            CurrencyCode target = CurrencyCode.valueOf(to.toUpperCase());
            
            List<ExchangeRate> history = exchangeRateService.getExchangeRateHistory(
                    source, target, limit);
            
            List<ExchangeRateHistoryDTO> response = history.stream()
                    .map(rate -> ExchangeRateHistoryDTO.builder()
                            .id(rate.getId())
                            .sourceCurrency(rate.getSourceCurrency().name())
                            .targetCurrency(rate.getTargetCurrency().name())
                            .rate(rate.getRate())
                            .bid(rate.getBid())
                            .ask(rate.getAsk())
                            .provider(rate.getProvider())
                            .createdAt(rate.getCreatedAt())
                            .expiresAt(rate.getExpiresAt())
                            .build())
                    .collect(Collectors.toList());
            
            log.info("[FX_HISTORY_API_SUCCESS] Returned {} historical rates", response.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[FX_HISTORY_API_ERROR] Invalid currency: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get specific exchange rate between two currencies.
     * 
     * GET /api/exchange-rates/{from}/{to}
     * 
     * Example: GET /api/exchange-rates/USD/EUR
     * 
     * Alternative to query param version (/api/exchange-rates?from=USD&to=EUR)
     * Cleaner URL for direct currency pair lookups.
     * 
     * Response: Same as GET /api/exchange-rates with query params
     * 
     * @param from Source currency (ISO code)
     * @param to Target currency (ISO code)
     * @return Exchange rate with bid/ask
     */
    @GetMapping("/{from}/{to}")
    public ResponseEntity<ExchangeRateResponse> getSpecificExchangeRate(
            @PathVariable String from,
            @PathVariable String to) {
        
        log.info("[FX_SPECIFIC_API] Request: {} → {}", from, to);
        
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
            
            log.info("[FX_SPECIFIC_API_SUCCESS] Rate: {} (provider: {})", 
                    rate.getRate(), rate.getProvider());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[FX_SPECIFIC_API_ERROR] Invalid currency code: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
