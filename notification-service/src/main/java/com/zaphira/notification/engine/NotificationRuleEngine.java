package com.zaphira.notification.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RuleEngine - loads and manages notification rules from YAML configuration
 * All notification decisions are data-driven, not hardcoded
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "notification.rules")
@RequiredArgsConstructor
public class NotificationRuleEngine {
    
    private Map<String, NotificationRule> rules = new HashMap<>();
    
    /**
     * Get rule for a specific event type
     */
    public NotificationRule getRule(String eventType) {
        NotificationRule rule = rules.get(eventType);
        if (rule == null) {
            log.warn("No notification rule found for event type: {}", eventType);
            // Return disabled rule as fallback
            return NotificationRule.builder()
                .eventType(eventType)
                .enabled(false)
                .build();
        }
        return rule;
    }
    
    /**
     * Check if event type has notification enabled
     */
    public boolean isNotificationEnabled(String eventType) {
        NotificationRule rule = getRule(eventType);
        return rule != null && rule.isApplicable();
    }
    
    /**
     * Register or update a rule
     */
    public void registerRule(String eventType, NotificationRule rule) {
        rules.put(eventType, rule);
        log.info("Registered notification rule for event type: {}", eventType);
    }
    
    /**
     * Get all configured rules
     */
    public Map<String, NotificationRule> getAllRules() {
        return new HashMap<>(rules);
    }
    
    /**
     * Set rules from configuration
     */
    public void setRules(Map<String, NotificationRule> rules) {
        this.rules = rules;
        log.info("Loaded {} notification rules from configuration", rules.size());
    }
}
