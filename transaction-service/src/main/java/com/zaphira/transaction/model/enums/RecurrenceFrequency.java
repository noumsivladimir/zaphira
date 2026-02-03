package com.zaphira.transaction.model.enums;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * RecurrenceFrequency - How often a recurring transaction executes.
 */
public enum RecurrenceFrequency {
    /**
     * Every day.
     */
    DAILY(1, ChronoUnit.DAYS),

    /**
     * Every week (7 days).
     */
    WEEKLY(7, ChronoUnit.DAYS),

    /**
     * Every 2 weeks (14 days).
     */
    BI_WEEKLY(14, ChronoUnit.DAYS),

    /**
     * Every month.
     */
    MONTHLY(1, ChronoUnit.MONTHS),

    /**
     * Every 3 months.
     */
    QUARTERLY(3, ChronoUnit.MONTHS),

    /**
     * Every year.
     */
    YEARLY(1, ChronoUnit.YEARS);

    private final int amount;
    private final ChronoUnit unit;

    RecurrenceFrequency(int amount, ChronoUnit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    /**
     * Calculate next run date from given start date.
     */
    public LocalDateTime calculateNextRun(LocalDateTime from) {
        return from.plus(amount, unit);
    }

    public int getAmount() {
        return amount;
    }

    public ChronoUnit getUnit() {
        return unit;
    }
}
