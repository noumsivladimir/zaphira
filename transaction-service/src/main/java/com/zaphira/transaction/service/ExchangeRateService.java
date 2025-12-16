package com.zaphira.transaction.service;

import com.zaphira.transaction.exception.ExchangeRateException;
import com.zaphira.transaction.model.ExchangeRate;
import com.zaphira.transaction.model.enums.CurrencyCode;
import com.zaphira.transaction.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ExchangeRateService - FX rate management with caching.
 * 
 * Responsibilities:
 * 1. Fetch and cache exchange rates
 * 2. Provide rate lookups with TTL
 * 3. Handle rate fallback/staleness
 * 4. Scheduled cache refresh
 * 
 * Caching Strategy:
 * - @Cacheable for rate lookups (Redis)
 * - Cache TTL: 24 hours (configurable)
 * - Automatic refresh 1 hour before expiry
 * - Fallback to stale rates if provider unavailable
 * 
 * Provider Integration:
 * - Primary: ECB (European Central Bank) - Free, reliable
 * - Fallback: Open Exchange Rates, Fixer.io
 * 
 * Patterns (from Phase 2):
 * - @Transactional for rate updates
 * - Scheduled tasks for cache refresh
 * - Exception handling with specific error types
 * - Logging for audit trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate;
    
    @Value("${fx.provider:ECB}")
    private String fxProvider;
    
    @Value("${fx.cache.ttl-hours:24}")
    private Integer cacheTtlHours;
    
    // ============================================================
    // RATE LOOKUP & CACHING
    // ============================================================
    
    /**
     * Get current exchange rate (cached).
     * 
     * Caching behavior:
     * 1. Check cache first
     * 2. Return if found and not expired
     * 3. Fetch from provider if expired
     * 4. Cache result
     * 5. Return with TTL
     * 
     * @param source Source currency
     * @param target Target currency
     * @return Exchange rate (1 unit of source = X units of target)
     * @throws ExchangeRateException if rate unavailable
     */
    @Cacheable(value = "exchangeRates", key = "#source.name() + '_' + #target.name()", 
               cacheManager = "cacheManager")
    public ExchangeRate getExchangeRate(CurrencyCode source, CurrencyCode target) {
        log.debug("[FX_LOOKUP] Fetching rate: {} → {}", source, target);
        
        // Try to find valid cached rate first
        var cachedRate = exchangeRateRepository.findLatestValidRate(source, target);
        if (cachedRate.isPresent()) {
            log.info("[FX_CACHE_HIT] Rate found (expires in {})", cachedRate.get().getExpiresAt());
            return cachedRate.get();
        }
        
        // Rate not in cache or expired, fetch from provider
        log.info("[FX_FETCH] Fetching from provider: {}", fxProvider);
        ExchangeRate rate = fetchRateFromProvider(source, target);
        
        if (rate == null) {
            // Fallback to most recent rate (even if stale)
            var stalerate = exchangeRateRepository.findMostRecent(source, target);
            if (stalerate.isPresent()) {
                log.warn("[FX_FALLBACK] Using stale rate (expired at {})", stalerate.get().getExpiresAt());
                return stalerate.get();
            }
            
            log.error("[FX_ERROR] No rate available for {} → {}", source, target);
            throw new ExchangeRateException(
                "Exchange rate not available for " + source + " → " + target
            );
        }
        
        // Save and return
        ExchangeRate saved = exchangeRateRepository.save(rate);
        log.info("[FX_CACHED] Rate cached: {} (expires at {})", rate.getRate(), rate.getExpiresAt());
        return saved;
    }
    
    /**
     * Fetch rate from external provider.
     * 
     * This would integrate with:
     * - European Central Bank (ECB) - Free, EUR base
     * - Open Exchange Rates - Paid, comprehensive
     * - Fixer.io - Paid, reliable
     * 
     * @param source Source currency
     * @param target Target currency
     * @return ExchangeRate entity or null if fetch fails
     */
    private ExchangeRate fetchRateFromProvider(CurrencyCode source, CurrencyCode target) {
        try {
            // TODO: Implement actual provider integration
            // For now, return mock rate for testing
            BigDecimal mockRate = BigDecimal.valueOf(0.92); // USD to EUR example
            
            return ExchangeRate.builder()
                .sourceCurrency(source)
                .targetCurrency(target)
                .rate(mockRate)
                .bid(mockRate.subtract(BigDecimal.valueOf(0.0005)))
                .ask(mockRate.add(BigDecimal.valueOf(0.0005)))
                .provider(fxProvider)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(cacheTtlHours))
                .build();
        } catch (Exception e) {
            log.error("[FX_PROVIDER_ERROR] Failed to fetch rate: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert amount from source to target currency.
     * 
     * @param amount Amount in source currency
     * @param source Source currency
     * @param target Target currency
     * @return Amount in target currency
     * @throws ExchangeRateException if rate unavailable
     */
    public BigDecimal convert(BigDecimal amount, CurrencyCode source, CurrencyCode target) {
        if (source == target) {
            return amount; // No conversion needed
        }
        
        ExchangeRate rate = getExchangeRate(source, target);
        log.info("[FX_CONVERT] {} {} → {} (rate: {})", amount, source, target, rate.getRate());
        return rate.convert(amount);
    }
    
    // ============================================================
    // CACHE REFRESH & MAINTENANCE
    // ============================================================
    
    /**
     * Scheduled task to refresh rates before expiry.
     * 
     * Runs every 12 hours to refresh rates expiring in next 1 hour.
     * This prevents cache misses at request time.
     */
    @Scheduled(fixedRateString = "${fx.refresh-interval:43200000}") // 12 hours
    public void refreshExpiringRates() {
        log.info("[FX_REFRESH] Starting scheduled rate refresh");
        
        var expiringRates = exchangeRateRepository.findSoonToExpireRates(
            LocalDateTime.now().plusHours(1)
        );
        
        log.info("[FX_REFRESH] Found {} rates expiring soon", expiringRates.size());
        
        for (ExchangeRate expired : expiringRates) {
            try {
                ExchangeRate newRate = fetchRateFromProvider(
                    expired.getSourceCurrency(),
                    expired.getTargetCurrency()
                );
                
                if (newRate != null) {
                    exchangeRateRepository.save(newRate);
                    evictRate(expired.getSourceCurrency(), expired.getTargetCurrency());
                }
            } catch (Exception e) {
                log.warn(
                    "[FX_REFRESH_ERROR] Failed to refresh rate {}-{}: {}",
                    expired.getSourceCurrency(), expired.getTargetCurrency(), e.getMessage()
                );
            }
        }
        
        log.info("[FX_REFRESH] Completed");
    }
    
    /**
     * Clear specific rate from cache.
     * 
     * @param source Source currency
     * @param target Target currency
     */
    @CacheEvict(value = "exchangeRates", key = "#source.name() + '_' + #target.name()")
    public void evictRate(CurrencyCode source, CurrencyCode target) {
        log.debug("[FX_CACHE_EVICT] Cleared cache for {} → {}", source, target);
    }
    
    /**
     * Clear all exchange rates from cache.
     */
    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void clearAllRates() {
        log.info("[FX_CACHE_CLEAR] Cleared all exchange rates");
    }
    
    /**
     * Delete expired rates from database.
     * 
     * Runs periodically to clean up old rate records.
     */
    @Scheduled(fixedRateString = "${fx.cleanup-interval:3600000}") // 1 hour
    public void cleanupExpiredRates() {
        var expiredRates = exchangeRateRepository.findExpiredRates();
        if (!expiredRates.isEmpty()) {
            exchangeRateRepository.deleteAll(expiredRates);
            log.info("[FX_CLEANUP] Deleted {} expired rates", expiredRates.size());
        }
    }
}
