package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** VO rules: Slug lowercase hyphen-separated, auto-generate từ name (BR-P02). */
class SlugTest {

    @Test
    void fromName_stripsVietnameseDiacritics() {
        assertEquals("ao-thun-nam-basic", Slug.fromName("Áo Thun Nam  Basic", 220).getValue());
    }

    @Test
    void fromName_truncatesToMaxLength() {
        String slug = Slug.fromName("a".repeat(300), 120).getValue();
        assertTrue(slug.length() <= 120);
    }

    @Test
    void of_rejectsInvalidFormat() {
        assertThrows(BusinessException.class, () -> Slug.of("Có Dấu Cách", 120));
        assertThrows(BusinessException.class, () -> Slug.of("-leading-hyphen", 120));
    }

    @Test
    void of_acceptsValidSlug() {
        assertEquals("ao-thun-2", Slug.of("Ao-Thun-2", 120).getValue());
    }
}
