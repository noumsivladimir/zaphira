package com.zaphira.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s with id %d not found", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

