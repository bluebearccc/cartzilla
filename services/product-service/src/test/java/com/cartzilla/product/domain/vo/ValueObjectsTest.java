package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsTest {

    @Test
    @DisplayName("Money: hợp lệ khi >= 0 và làm tròn 2 chữ số thập phân")
    void money_validAndScale() {
        Money m1 = Money.of(new BigDecimal("150.506"));
        assertEquals(new BigDecimal("150.51"), m1.getAmount());

        Money m2 = Money.ZERO;
        assertEquals(new BigDecimal("0.00"), m2.getAmount());

        assertThrows(BusinessException.class, () -> Money.of(new BigDecimal("-1.00")));
    }

    @Test
    @DisplayName("Sku: hợp lệ khi đúng định dạng alphanumeric/hyphen và được uppercase")
    void sku_validAndNormalized() {
        Sku sku = Sku.of("sku-abc-123");
        assertEquals("SKU-ABC-123", sku.getValue());

        assertThrows(BusinessException.class, () -> Sku.of(""));
        assertThrows(BusinessException.class, () -> Sku.of("SKU@123#"));
    }

    @Test
    @DisplayName("ColorHex: hợp lệ khi đúng định dạng #RRGGBB")
    void colorHex_validFormat() {
        ColorHex hex = ColorHex.of("#ff0000");
        assertEquals("#FF0000", hex.getValue());

        assertThrows(BusinessException.class, () -> ColorHex.of("FF0000"));
        assertThrows(BusinessException.class, () -> ColorHex.of("#GGGGGG"));
        assertThrows(BusinessException.class, () -> ColorHex.of("#123"));
    }
}
