package com.zaphira.notification.model.entity;

/**
 * Notification priority - determines display order and retry strategy
 */
public enum NotificationPriority {
    LOW,          // Information messages
    NORMAL,       // Standard notifications
    HIGH,         // Important alerts (failed transactions, account issues)
    CRITICAL      // Security alerts, account locked, etc.
}
