package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

/** VO: Slug — lowercase, hyphen-separated. */
public final class Slug {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final String value;

    private Slug(String value) { this.value = value; }

    public static Slug of(String raw, int maxLength) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("Slug must not be blank");
        String s = raw.trim().toLowerCase();
        if (s.length() > maxLength)
            throw new BusinessException("Slug exceeds max length " + maxLength);
        if (!VALID.matcher(s).matches())
            throw new BusinessException("Invalid slug format: " + raw);
        return new Slug(s);
    }

    /** Auto-generate slug từ name */
    public static Slug fromName(String name, int maxLength) {
        if (name == null || name.isBlank())
            throw new BusinessException("Name must not be blank to generate slug");
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = DIACRITICS.matcher(normalized).replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (slug.length() > maxLength) slug = slug.substring(0, maxLength).replaceAll("-$", "");
        return new Slug(slug);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Slug s && value.equals(s.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
