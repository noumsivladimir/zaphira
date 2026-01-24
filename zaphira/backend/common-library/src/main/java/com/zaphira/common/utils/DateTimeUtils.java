package com.zaphira.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility class for date and time operations
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public final class DateTimeUtils {
    
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Get current LocalDateTime in UTC
     * 
     * @return Current UTC time
     */
    public static LocalDateTime nowUTC() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }
    
    /**
     * Convert LocalDateTime to Date
     * 
     * @param localDateTime LocalDateTime to convert
     * @return Date object
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Convert Date to LocalDateTime
     * 
     * @param date Date to convert
     * @return LocalDateTime object
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Format LocalDateTime to string
     * 
     * @param dateTime LocalDateTime to format
     * @param pattern Format pattern
     * @return Formatted string
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Format LocalDateTime with default datetime format
     * 
     * @param dateTime LocalDateTime to format
     * @return Formatted string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_DATETIME_FORMAT);
    }
    
    /**
     * Parse string to LocalDateTime
     * 
     * @param dateTimeString String to parse
     * @param pattern Format pattern
     * @return LocalDateTime object
     */
    public static LocalDateTime parse(String dateTimeString, String pattern) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Check if a LocalDateTime is in the past
     * 
     * @param dateTime LocalDateTime to check
     * @return true if in the past, false otherwise
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if a LocalDateTime is in the future
     * 
     * @param dateTime LocalDateTime to check
     * @return true if in the future, false otherwise
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now());
    }
    
    /**
     * Add minutes to current time
     * 
     * @param minutes Minutes to add
     * @return LocalDateTime after adding minutes
     */
    public static LocalDateTime plusMinutes(int minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }
    
    /**
     * Add hours to current time
     * 
     * @param hours Hours to add
     * @return LocalDateTime after adding hours
     */
    public static LocalDateTime plusHours(int hours) {
        return LocalDateTime.now().plusHours(hours);
    }
    
    /**
     * Add days to current time
     * 
     * @param days Days to add
     * @return LocalDateTime after adding days
     */
    public static LocalDateTime plusDays(int days) {
        return LocalDateTime.now().plusDays(days);
    }
}
