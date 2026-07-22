package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cartzilla.product.application.usecase.CreateCategoryUseCase;
import com.cartzilla.product.application.usecase.DeleteCategoryUseCase;
import com.cartzilla.product.application.usecase.ListCategoriesUseCase;
import com.cartzilla.product.application.usecase.UpdateCategoryUseCase;
import com.cartzilla.product.domain.entity.Category;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllersTest {

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ListCategoriesUseCase listCategoriesUseCase;
    @Mock
    private CreateCategoryUseCase createCategoryUseCase;
    @Mock
    private UpdateCategoryUseCase updateCategoryUseCase;
    @Mock
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @InjectMocks
    private CategoryController categoryController;

    @InjectMocks
    private AdminCategoryController adminCategoryController;

    private Category category;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        publicMockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminMockMvc = MockMvcBuilders.standaloneSetup(adminCategoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        categoryId = UUID.randomUUID();
        category = Category.create("Thời Trang", "thoi-trang", null, null, 0);
    }

    @Test
    @DisplayName("GET /api/categories — Public Lấy cây category active")
    void publicTree_success() throws Exception {
        when(listCategoriesUseCase.execute(false)).thenReturn(List.of(category));

        publicMockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Thời Trang"));
    }

    @Test
    @DisplayName("POST /api/admin/categories — Admin tạo category mới")
    void adminCreate_success() throws Exception {
        when(createCategoryUseCase.execute(any())).thenReturn(category);

        Map<String, Object> reqBody = Map.of(
                "name", "Thời Trang",
                "slug", "thoi-trang",
                "sortOrder", 0
        );

        adminMockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo category thành công"));
    }

    @Test
    @DisplayName("PUT /api/admin/categories/{id} — Admin cập nhật category")
    void adminUpdate_success() throws Exception {
        when(updateCategoryUseCase.execute(eq(categoryId), any())).thenReturn(category);

        Map<String, Object> reqBody = Map.of(
                "name", "Thời Trang Nam",
                "slug", "thoi-trang-nam",
                "sortOrder", 1,
                "active", true
        );

        adminMockMvc.perform(put("/api/admin/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật category thành công"));
    }

    @Test
    @DisplayName("DELETE /api/admin/categories/{id} — Admin xóa category")
    void adminDelete_success() throws Exception {
        doNothing().when(deleteCategoryUseCase).execute(categoryId);

        adminMockMvc.perform(delete("/api/admin/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa category"));
    }
}
