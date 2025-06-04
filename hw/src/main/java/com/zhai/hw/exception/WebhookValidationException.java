package com.zhai.hw.exception;

/**
 * Exception thrown when GitHub webhook validation fails.
 */
public class WebhookValidationException extends RuntimeException {
    
    public WebhookValidationException(String message) {
        super(message);
    }
    
    public WebhookValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
