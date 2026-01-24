package com.zaphira.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API Response wrapper for all REST endpoints
 * 
 * @param <T> The type of data being returned
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Indicates if the request was successful
     */
    private boolean success;
    
    /**
     * Human-readable message about the result
     */
    private String message;
    
    /**
     * The actual response data (null on error)
     */
    private T data;
    
    /**
     * Additional metadata (e.g., pagination info, error details)
     */
    private Map<String, Object> metadata;
    
    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // ==================== SUCCESS RESPONSES ====================
    
    /**
     * Create a success response with data
     * 
     * @param data The response data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a success response with data and message
     * 
     * @param data The response data
     * @param message Success message
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a success response with data, message, and metadata
     * 
     * @param data The response data
     * @param message Success message
     * @param metadata Additional metadata
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .metadata(metadata)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a success response with only a message (no data)
     * 
     * @param message Success message
     * @return ApiResponse with success=true and no data
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // ==================== ERROR RESPONSES ====================
    
    /**
     * Create an error response with message
     * 
     * @param message Error message
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create an error response with message and metadata (e.g., validation errors)
     * 
     * @param message Error message
     * @param metadata Additional error details
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .metadata(metadata)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create an error response from an exception
     * 
     * @param exception The exception that occurred
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(Exception exception) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(exception.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Add metadata to the response
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This ApiResponse instance for chaining
     */
    public ApiResponse<T> addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Check if the response is successful
     * 
     * @return true if success, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Check if the response is an error
     * 
     * @return true if error, false otherwise
     */
    public boolean isError() {
        return !success;
    }
}
