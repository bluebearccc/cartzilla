package com.cartzilla.user.domain.exception;

import com.cartzilla.web.exception.BusinessException;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException(String email) {
        super("Email đã tồn tại: " + email);
    }
}
