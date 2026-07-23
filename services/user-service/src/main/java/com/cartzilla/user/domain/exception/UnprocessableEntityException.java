package com.cartzilla.user.domain.exception;

import com.cartzilla.web.exception.BusinessException;

public class UnprocessableEntityException extends BusinessException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
