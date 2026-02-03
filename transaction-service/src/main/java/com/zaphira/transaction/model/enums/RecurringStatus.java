package com.zaphira.transaction.model.enums;

/**
 * RecurringStatus - Status of a recurring transaction.
 */
public enum RecurringStatus {
    /**
     * Pending first execution.
     */
    PENDING,

    /**
     * Currently active and executing on schedule.
     */
    ACTIVE,

    /**
     * Temporarily paused by user.
     */
    PAUSED,

    /**
     * Completed - reached end date or max executions.
     */
    COMPLETED,

    /**
     * Cancelled by user.
     */
    CANCELLED,

    /**
     * Failed - too many consecutive failures.
     */
    FAILED
}
