package com.cartzilla.product.domain.exception;

/** Resource không tồn tại hoặc không visible với caller (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
