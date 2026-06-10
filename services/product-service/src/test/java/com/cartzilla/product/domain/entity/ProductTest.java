package com.cartzilla.product.domain.entity;

import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Domain rules: PA-01, PA-05, PA-07, P-02, P-05, PV-02..PV-05. */
class ProductTest {

    private Product newProduct() {
        return Product.create(UUID.randomUUID(), "Áo thun nam", "ao-thun-nam",
                "Cotton 100%", new BigDecimal("199000"), null, "ao,thun");
    }

    @Test
    void create_rejectsNegativeBasePrice() {
        assertThrows(BusinessException.class, () -> Product.create(
                UUID.randomUUID(), "Áo", "ao", null, new BigDecimal("-1"), null, null));
    }

    @Test
    void addVariant_rejectsDuplicateSizeColorCombo_PV05() {
        Product p = newProduct();
        p.addVariant(ProductVariant.create("SKU-1", "M", "Đen", null, BigDecimal.TEN, 5));
        assertThrows(BusinessException.class, () ->
                p.addVariant(ProductVariant.create("SKU-2", "m", "đen", null, BigDecimal.TEN, 5)));
    }

    @Test
    void reserveStock_rejectsInsufficient_PA07() {
        ProductVariant v = ProductVariant.create("SKU-1", "M", "Đen", null, BigDecimal.TEN, 3);
        assertThrows(BusinessException.class, () -> v.reserveStock(5));
        assertEquals(3, v.getStock()); // stock không đổi khi reject
        v.reserveStock(3);
        assertEquals(0, v.getStock());
        v.releaseStock(2);
        assertEquals(2, v.getStock());
    }

    @Test
    void setFeatured_rejectsWhenInactive_P05() {
        Product p = newProduct();
        p.deactivate();
        assertThrows(BusinessException.class, () -> p.setFeatured(true));
    }

    @Test
    void deactivate_clearsFeatured() {
        Product p = newProduct();
        p.setFeatured(true);
        p.deactivate();
        assertFalse(p.isFeatured());
    }

    @Test
    void addImage_keepsSinglePrimary_PA05() {
        Product p = newProduct();
        p.addImage(ProductImage.create("http://img/1.jpg", null, true, 0));
        p.addImage(ProductImage.create("http://img/2.jpg", null, true, 1));
        long primaryCount = p.getImages().stream().filter(ProductImage::isPrimary).count();
        assertEquals(1, primaryCount);
        assertTrue(p.getImages().get(1).isPrimary()); // ảnh mới là primary
    }

    @Test
    void ensurePrimaryImage_promotesFirstWhenNonePrimary_PA05() {
        Product p = newProduct();
        p.addImage(ProductImage.create("http://img/1.jpg", null, false, 0));
        p.addImage(ProductImage.create("http://img/2.jpg", null, false, 1));
        p.ensurePrimaryImage();
        assertTrue(p.getImages().get(0).isPrimary());
    }

    @Test
    void isSellable_requiresVariantAndImage_PA01() {
        Product p = newProduct();
        assertFalse(p.isSellable());
        p.addVariant(ProductVariant.create("SKU-1", "M", "Đen", null, BigDecimal.TEN, 5));
        assertFalse(p.isSellable()); // chưa có ảnh
        p.addImage(ProductImage.create("http://img/1.jpg", null, true, 0));
        assertTrue(p.isSellable());
        p.deactivate();
        assertFalse(p.isSellable());
    }
}
