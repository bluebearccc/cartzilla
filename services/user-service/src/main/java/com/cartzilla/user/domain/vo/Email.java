package com.cartzilla.user.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.util.Objects;
import java.util.regex.Pattern;

/** VO: Email — RFC 5322 simplified; normalize lowercase; max 255. */
public final class Email {

    private static final Pattern PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("Email must not be blank");
        String normalized = raw.trim().toLowerCase();
        if (normalized.length() > 255)
            throw new BusinessException("Email must not exceed 255 characters");
        if (!PATTERN.matcher(normalized).matches())
            throw new BusinessException("Invalid email format: " + raw);
        return new Email(normalized);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Email e && value.equals(e.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
