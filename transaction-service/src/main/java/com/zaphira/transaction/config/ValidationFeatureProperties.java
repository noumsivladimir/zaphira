package com.zaphira.transaction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Feature flag configuration for async Kafka validation
 * Controls whether transactions use Kafka-based async validation or synchronous processing
 * 
 * Default: DISABLED (safe mode) - all transactions processed synchronously
 * Enabled: Transactions validated asynchronously via Kafka (production rollout)
 */
@Component
@ConfigurationProperties(prefix = "validation.feature")
@Data
public class ValidationFeatureProperties {
    
    /**
     * Enable/disable async Kafka validation
     * Default: false (disabled for safe deployment)
     */
    private boolean enabled = false;
    
    /**
     * Percentage of traffic to route through Kafka validation (0-100)
     * Only applies when enabled=true
     * Allows gradual canary rollout
     * Default: 0 (no traffic)
     */
    private int trafficPercentage = 0;
    
    /**
     * Timeout for validation result in seconds
     * If validation doesn't complete within this time, transaction moves to EXPIRED status
     * Default: 300 seconds (5 minutes)
     */
    private int validationTimeoutSeconds = 300;
    
    /**
     * Enable automatic cleanup of expired validations
     * Default: true
     */
    private boolean enableAutoCleanup = true;
}
