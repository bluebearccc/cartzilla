package com.cartzilla.product.domain.entity;

import com.cartzilla.product.domain.vo.VendorType;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VendorTest {

    private void setId(Vendor vendor, UUID id) throws Exception {
        Field field = Vendor.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(vendor, id);
    }

    @Test
    @DisplayName("Tạo vendor thành công với thông tin hợp lệ")
    void create_success() throws Exception {
        Vendor vendor = Vendor.create("Nike Store", "nike-store", VendorType.BRAND,
                "contact@nike.com", "0901234567", "https://nike.com", "https://logo.com/nike.png");
        setId(vendor, UUID.randomUUID());

        assertNotNull(vendor.getId());
        assertEquals("Nike Store", vendor.getName());
        assertEquals("nike-store", vendor.getSlug());
        assertEquals(VendorType.BRAND, vendor.getVendorType());
        assertEquals("contact@nike.com", vendor.getContactEmail());
        assertEquals("0901234567", vendor.getPhone());
        assertEquals("https://nike.com", vendor.getWebsite());
        assertEquals("https://logo.com/nike.png", vendor.getLogoUrl());
        assertTrue(vendor.isActive());
        assertFalse(vendor.isDeleted());
    }

    @Test
    @DisplayName("Không cho phép tạo vendor với tên rỗng")
    void create_rejectsBlankName() {
        assertThrows(BusinessException.class, () -> Vendor.create("", "nike", VendorType.BRAND, null, null, null, null));
    }

    @Test
    @DisplayName("Cập nhật thông tin vendor")
    void update_success() {
        Vendor vendor = Vendor.create("Adidas", "adidas", VendorType.BRAND, null, null, null, null);
        vendor.update("Adidas Official", VendorType.BRAND, "info@adidas.com", "0900000000", "https://adidas.com", "https://logo.png");
        vendor.changeSlug("adidas-official");
        vendor.deactivate();

        assertEquals("Adidas Official", vendor.getName());
        assertEquals("adidas-official", vendor.getSlug());
        assertFalse(vendor.isActive());
    }

    @Test
    @DisplayName("Soft delete vendor đánh dấu isDeleted = true và isActive = false")
    void softDelete_success() {
        Vendor vendor = Vendor.create("Puma", "puma", VendorType.BRAND, null, null, null, null);
        vendor.deactivate();
        vendor.softDelete();

        assertTrue(vendor.isDeleted());
        assertFalse(vendor.isActive());
    }
}
