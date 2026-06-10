package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.util.Objects;
import java.util.regex.Pattern;

/** VO: Sku — uppercase alphanumeric + '-', max 50, unique. BR-P03. */
public final class Sku {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9-]{0,49}$");

    private final String value;

    private Sku(String value) { this.value = value; }

    public static Sku of(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("SKU must not be blank");
        String normalized = raw.trim().toUpperCase();
        if (normalized.length() > 50)
            throw new BusinessException("SKU must not exceed 50 characters (BR-P03)");
        if (!PATTERN.matcher(normalized).matches())
            throw new BusinessException("Invalid SKU format (BR-P03): " + raw);
        return new Sku(normalized);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Sku s && value.equals(s.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
