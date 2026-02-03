package com.zaphira.transaction.model;

import com.zaphira.transaction.model.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ExchangeRate - Cached FX rates.
 * 
 * Purpose:
 * - Store exchange rates with timestamps
 * - Enable historical FX rate lookups
 * - Support rate caching with TTL (time-to-live)
 * 
 * Example:
 * sourceCurrency: USD, targetCurrency: EUR
 * rate: 0.92 (1 USD = 0.92 EUR)
 * 
 * Pattern (from Phase 2):
 * - @Transactional for atomic updates
 * - Timestamp tracking (created_at, updated_at, expires_at)
 * - Indexed on source/target for quick lookups
 */
@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_exchange_rates_src_tgt", columnList = "source_currency,target_currency"),
    @Index(name = "idx_exchange_rates_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyCode sourceCurrency;
    
    @Column(name = "target_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyCode targetCurrency;
    
    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;
    
    @Column(name = "bid", precision = 19, scale = 8)
    private BigDecimal bid;
    
    @Column(name = "ask", precision = 19, scale = 8)
    private BigDecimal ask;
    
    @Column(name = "provider", nullable = false)
    private String provider; // "ECB", "OPENEXCHANGERATES", "FIXER", etc.
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // TTL for cache
    
    /**
     * Check if this rate has expired (stale cache).
     * 
     * @return true if rate is no longer valid
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Convert amount from source to target currency.
     * 
     * @param amount Amount in source currency
     * @return Amount in target currency
     */
    public BigDecimal convert(BigDecimal amount) {
        if (this.isExpired()) {
            throw new IllegalStateException("Exchange rate has expired");
        }
        return amount.multiply(this.rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusHours(24); // Default 24h TTL
    }
}
