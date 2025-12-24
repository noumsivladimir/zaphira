package com.zaphira.notification.model.entity;

/**
 * Notification channels - supported delivery mechanisms
 */
public enum NotificationChannel {
    IN_APP,       // In-application notification (database + API)
    EMAIL,        // Email delivery
    SMS,          // SMS delivery
    PUSH          // Push notification
}
