package com.zaphira.common.exception;

/**
 * Exception thrown when a requested resource is not found
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
public class ResourceNotFoundException extends BusinessException {
    
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), "NOT_FOUND");
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s with id %d not found", resource, id), "NOT_FOUND");
        this.resourceName = resource;
        this.fieldName = "id";
        this.fieldValue = id;
    }
    
    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND");
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}

