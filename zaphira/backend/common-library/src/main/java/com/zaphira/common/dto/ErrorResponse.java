package com.zaphira.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed error response for exception handling
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error code for client-side handling (e.g., "USER_NOT_FOUND")
     */
    private String code;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Detailed error description (optional)
     */
    private String details;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Timestamp of the error
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Validation errors (field-specific)
     */
    private Map<String, String> fieldErrors;
    
    /**
     * List of sub-errors or related errors
     */
    private List<SubError> errors;
    
    /**
     * Sub-error structure for nested errors
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
    
    /**
     * Create a simple error response
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param path Request path
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .message(message)
            .path(path)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create an error response with code
     * 
     * @param status HTTP status code
     * @param code Error code
     * @param message Error message
     * @param path Request path
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String code, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .code(code)
            .message(message)
            .path(path)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create an error response with validation errors
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param path Request path
     * @param fieldErrors Field validation errors
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String message, String path, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
            .status(status)
            .message(message)
            .path(path)
            .fieldErrors(fieldErrors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
