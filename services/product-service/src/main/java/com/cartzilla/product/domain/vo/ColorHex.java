package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.util.Objects;
import java.util.regex.Pattern;

/** VO: ColorHex — pattern #RRGGBB, max 10. */
public final class ColorHex {

    private static final Pattern PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final String value;

    private ColorHex(String value) { this.value = value; }

    public static ColorHex of(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("ColorHex must not be blank");
        String s = raw.trim().toUpperCase();
        if (s.length() > 10)
            throw new BusinessException("ColorHex must not exceed 10 characters");
        if (!PATTERN.matcher(raw.trim()).matches())
            throw new BusinessException("Invalid color hex format (must be #RRGGBB): " + raw);
        return new ColorHex(s);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof ColorHex c && value.equalsIgnoreCase(c.value);
    }
    @Override public int hashCode() { return Objects.hash(value.toUpperCase()); }
    @Override public String toString() { return value; }
}
