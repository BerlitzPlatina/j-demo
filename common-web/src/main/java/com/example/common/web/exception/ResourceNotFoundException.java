package com.example.common.web.exception;

/**
 * Thrown when a requested resource does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
