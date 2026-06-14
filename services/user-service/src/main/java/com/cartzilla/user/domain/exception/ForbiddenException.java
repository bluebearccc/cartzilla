package com.cartzilla.user.domain.exception;

import com.cartzilla.web.exception.BusinessException;

/**
 * Vi phạm phân quyền / trạng thái tài khoản → HTTP 403.
 * Ví dụ: user inactive đăng nhập (SRS TC-04), thiếu role ADMIN.
 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message);
    }
}
