package com.cartzilla.user.domain.exception;

import com.cartzilla.web.exception.BusinessException;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message);
    }
}
