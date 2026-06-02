package com.cartzilla.web.exception;

/** Exception nghiệp vụ chung (HTTP 400/409). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
