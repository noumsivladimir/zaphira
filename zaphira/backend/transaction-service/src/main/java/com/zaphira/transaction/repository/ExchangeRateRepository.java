package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.ExchangeRate;
import com.zaphira.transaction.model.enums.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ExchangeRateRepository - Cached FX rates data access.
 * 
 * Queries:
 * 1. findLatestRate - Get most recent valid rate
 * 2. findExpiredRates - Identify stale cache
 * 3. findBySourceAndTarget - Direct lookup
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    /**
     * Find the most recent non-expired exchange rate.
     * 
     * @param source Source currency
     * @param target Target currency
     * @return Optional containing rate if found and not expired
     */
    @Query("SELECT e FROM ExchangeRate e " +
           "WHERE e.sourceCurrency = :source " +
           "AND e.targetCurrency = :target " +
           "AND e.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY e.updatedAt DESC " +
           "LIMIT 1")
    Optional<ExchangeRate> findLatestValidRate(
        @Param("source") CurrencyCode source,
        @Param("target") CurrencyCode target
    );
    
    /**
     * Find by source and target, regardless of expiration.
     * 
     * @param source Source currency
     * @param target Target currency
     * @return Optional containing most recent rate
     */
    @Query("SELECT e FROM ExchangeRate e " +
           "WHERE e.sourceCurrency = :source " +
           "AND e.targetCurrency = :target " +
           "ORDER BY e.updatedAt DESC " +
           "LIMIT 1")
    Optional<ExchangeRate> findMostRecent(
        @Param("source") CurrencyCode source,
        @Param("target") CurrencyCode target
    );
    
    /**
     * Find all expired rates for cleanup.
     * 
     * @return List of expired rates
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.expiresAt <= CURRENT_TIMESTAMP")
    List<ExchangeRate> findExpiredRates();
    
    /**
     * Find all rates expiring within next N minutes.
     * 
     * @param expiresAfter Threshold timestamp
     * @return List of soon-to-expire rates
     */
    @Query("SELECT e FROM ExchangeRate e " +
           "WHERE e.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expiresAfter")
    List<ExchangeRate> findSoonToExpireRates(@Param("expiresAfter") LocalDateTime expiresAfter);
}
