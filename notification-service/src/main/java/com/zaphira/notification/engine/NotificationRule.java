package com.zaphira.notification.engine;

import com.zaphira.notification.model.entity.NotificationChannel;
import com.zaphira.notification.model.entity.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * NotificationRule - configuration for determining how to handle a specific event type
 * Rules are loaded from YAML and can be modified without code changes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRule {
    
    private String eventType;                    // WalletBalanceUpdated, TransactionFailed, etc.
    private Boolean enabled;                     // Enable/disable notification for this event
    private List<NotificationChannel> channels;  // IN_APP, EMAIL, SMS, PUSH
    private String template;                     // Template name/ID to use
    private NotificationPriority priority;       // LOW, NORMAL, HIGH, CRITICAL
    private Integer maxRetries;                  // Max retry attempts for failed sends
    private Long retryDelayMs;                   // Delay in ms between retries
    private Boolean requiresPreference;          // Whether user preference is required
    
    /**
     * Check if this rule is applicable (enabled)
     */
    public boolean isApplicable() {
        return enabled != null && enabled;
    }
    
    /**
     * Check if a channel is configured for this rule
     */
    public boolean supportsChannel(NotificationChannel channel) {
        return channels != null && channels.contains(channel);
    }
}
