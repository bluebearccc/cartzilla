package com.cartzilla.user.domain.exception;

import com.cartzilla.web.exception.BusinessException;

/**
 * Lỗi validate nghiệp vụ không xử lý được → HTTP 422.
 * Ví dụ: voucher không đủ tuổi tài khoản (BR-V07), vượt perUserLimit, audience mismatch.
 */
public class UnprocessableEntityException extends BusinessException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
