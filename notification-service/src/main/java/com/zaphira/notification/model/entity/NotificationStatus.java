package com.zaphira.notification.model.entity;

/**
 * Notification status - tracks notification lifecycle
 */
public enum NotificationStatus {
    PENDING,      // Created, waiting to be sent
    SENT,         // Successfully dispatched
    FAILED,       // Failed to send
    READ,         // User has read the notification
    ARCHIVED      // Archived by user
}
