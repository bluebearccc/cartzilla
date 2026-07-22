package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.CategoryCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CreateCategoryUseCase createCategoryUseCase;

    @InjectMocks
    private UpdateCategoryUseCase updateCategoryUseCase;

    @InjectMocks
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @InjectMocks
    private ListCategoriesUseCase listCategoriesUseCase;

    @Test
    @DisplayName("CreateCategoryUseCase: Tạo category thành công")
    void createCategory_success() {
        CategoryCommand.Create cmd = new CategoryCommand.Create("Thời trang", "thoi-trang", null, null, 1);
        when(categoryRepository.existsBySlug("thoi-trang")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = createCategoryUseCase.execute(cmd);

        assertNotNull(result);
        assertEquals("Thời trang", result.getName());
        assertEquals("thoi-trang", result.getSlug());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("CreateCategoryUseCase: Từ chối nếu slug trùng")
    void createCategory_duplicateSlug_throwsException() {
        CategoryCommand.Create cmd = new CategoryCommand.Create("Thời trang", "thoi-trang", null, null, 1);
        when(categoryRepository.existsBySlug("thoi-trang")).thenReturn(true);

        assertThrows(BusinessException.class, () -> createCategoryUseCase.execute(cmd));
    }

    @Test
    @DisplayName("CreateCategoryUseCase: Từ chối nếu parentCategory không tồn tại")
    void createCategory_nonExistentParent_throwsException() {
        UUID parentId = UUID.randomUUID();
        CategoryCommand.Create cmd = new CategoryCommand.Create("Child", "child", parentId, null, 1);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> createCategoryUseCase.execute(cmd));
    }

    @Test
    @DisplayName("UpdateCategoryUseCase: Cập nhật category thành công")
    void updateCategory_success() {
        UUID id = UUID.randomUUID();
        Category existing = Category.create("Old Name", "old-slug", null, null, 0);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlug("new-slug")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryCommand.Update cmd = new CategoryCommand.Update("New Name", "new-slug", null, null, 1, true);

        Category updated = updateCategoryUseCase.execute(id, cmd);

        assertEquals("New Name", updated.getName());
        assertEquals("new-slug", updated.getSlug());
    }

    @Test
    @DisplayName("DeleteCategoryUseCase: Xóa category thành công khi không có danh mục con và sản phẩm")
    void deleteCategory_success() {
        UUID id = UUID.randomUUID();
        Category category = Category.create("Category", "category", null, null, 0);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(id)).thenReturn(false);
        when(productRepository.existsActiveByCategoryId(id)).thenReturn(false);

        deleteCategoryUseCase.execute(id);

        assertTrue(category.isDeleted());
        assertFalse(category.isActive());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("DeleteCategoryUseCase: Thất bại nếu danh mục có sản phẩm phụ thuộc")
    void deleteCategory_hasProducts_throwsException() {
        UUID id = UUID.randomUUID();
        Category category = Category.create("Category", "category", null, null, 0);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(productRepository.existsActiveByCategoryId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> deleteCategoryUseCase.execute(id));
    }

    @Test
    @DisplayName("ListCategoriesUseCase: Lấy danh sách category cho public / admin")
    void listCategories_success() {
        Category c1 = Category.create("C1", "c1", null, null, 0);
        when(categoryRepository.findAllActive()).thenReturn(List.of(c1));

        List<Category> activeList = listCategoriesUseCase.execute(false);
        assertEquals(1, activeList.size());
        verify(categoryRepository).findAllActive();
    }
}
