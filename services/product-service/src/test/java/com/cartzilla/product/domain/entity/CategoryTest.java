package com.cartzilla.product.domain.entity;

import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private void setId(Category category, UUID id) throws Exception {
        Field field = Category.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(category, id);
    }

    @Test
    @DisplayName("Tạo category thành công với thông tin hợp lệ")
    void create_success() throws Exception {
        Category category = Category.create("Thời Trang Nam", "thoi-trang-nam", null, null, 1);
        setId(category, UUID.randomUUID());

        assertNotNull(category.getId());
        assertEquals("Thời Trang Nam", category.getName());
        assertEquals("thoi-trang-nam", category.getSlug());
        assertNull(category.getParentId());
        assertEquals(1, category.getSortOrder());
        assertTrue(category.isActive());
        assertFalse(category.isDeleted());
    }

    @Test
    @DisplayName("Không cho phép tạo category với tên rỗng")
    void create_rejectsBlankName() {
        assertThrows(BusinessException.class, () -> Category.create("  ", "slug", null, null, 0));
    }

    @Test
    @DisplayName("Cập nhật category thành công")
    void update_success() {
        Category category = Category.create("Thời Trang", "thoi-trang", null, null, 0);

        category.update("Thời Trang Nữ", "https://img.jpg", 2);

        assertEquals("Thời Trang Nữ", category.getName());
        assertEquals("https://img.jpg", category.getImageUrl());
        assertEquals(2, category.getSortOrder());
    }

    @Test
    @DisplayName("Không cho phép category tự làm cha của chính mình")
    void update_rejectsSelfAsParent() throws Exception {
        Category category = Category.create("Áo", "ao", null, null, 0);
        UUID id = UUID.randomUUID();
        setId(category, id);

        assertThrows(BusinessException.class, () -> category.setParent(id));
    }

    @Test
    @DisplayName("Soft delete category đánh dấu isDeleted = true và isActive = false")
    void softDelete_success() {
        Category category = Category.create("Giày", "giay", null, null, 0);
        category.deactivate();
        category.softDelete();

        assertTrue(category.isDeleted());
        assertFalse(category.isActive());
    }
}
