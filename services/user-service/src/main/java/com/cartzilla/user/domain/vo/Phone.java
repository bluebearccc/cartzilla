package com.cartzilla.user.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Phone {

    private static final Pattern PATTERN =
            Pattern.compile("^(\\+84|0)[0-9]{9,10}$");

    private final String value;

    private Phone(String value) { this.value = value; }

    public static Phone of(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("Phone must not be blank");
        String normalized = raw.trim();
        if (normalized.length() > 20)
            throw new BusinessException("Phone must not exceed 20 characters");
        if (!PATTERN.matcher(normalized).matches())
            throw new BusinessException("Invalid Vietnamese phone number: " + raw);
        return new Phone(normalized);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Phone p && value.equals(p.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
