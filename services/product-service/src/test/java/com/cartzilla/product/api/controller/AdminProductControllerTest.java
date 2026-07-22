package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cartzilla.product.application.usecase.*;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ListProductsUseCase listProductsUseCase;
    @Mock
    private GetProductUseCase getProductUseCase;
    @Mock
    private CreateProductUseCase createProductUseCase;
    @Mock
    private UpdateProductUseCase updateProductUseCase;
    @Mock
    private DeleteProductUseCase deleteProductUseCase;
    @Mock
    private ManageVariantUseCase manageVariantUseCase;
    @Mock
    private ManageImageUseCase manageImageUseCase;

    @InjectMocks
    private AdminProductController adminProductController;

    private Product product;
    private UUID productId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminProductController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        product = Product.create(categoryId, "Quần Jean", "quan-jean", "Denim", new BigDecimal("450000"), null, "jean");
        product.addVariant(ProductVariant.create("JEAN-01", "30", "Xanh", "#0000FF", new BigDecimal("450000"), 15));
        product.addImage(ProductImage.create("https://img/jean.jpg", "Quần jean", true, 0));
    }

    @Test
    @DisplayName("GET /api/admin/products — Admin lấy danh sách sản phẩm gồm cả inactive")
    void adminListProducts_success() throws Exception {
        when(listProductsUseCase.execute(any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Quần Jean"));
    }

    @Test
    @DisplayName("POST /api/admin/products — Admin tạo sản phẩm mới thành công")
    void adminCreateProduct_success() throws Exception {
        when(createProductUseCase.execute(any())).thenReturn(product);

        Map<String, Object> reqBody = Map.of(
                "categoryId", categoryId.toString(),
                "name", "Quần Jean",
                "slug", "quan-jean",
                "description", "Denim",
                "basePrice", 450000,
                "variants", List.of(Map.of(
                        "sku", "JEAN-01",
                        "size", "30",
                        "color", "Xanh",
                        "colorHex", "#0000FF",
                        "price", 450000,
                        "stock", 15
                ))
        );

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo sản phẩm thành công"));
    }

    @Test
    @DisplayName("PUT /api/admin/products/{id} — Admin cập nhật thông tin sản phẩm")
    void adminUpdateProduct_success() throws Exception {
        when(updateProductUseCase.execute(eq(productId), any())).thenReturn(product);

        Map<String, Object> reqBody = Map.of(
                "categoryId", categoryId.toString(),
                "name", "Quần Jean Mới",
                "slug", "quan-jean-moi",
                "basePrice", 500000,
                "active", true
        );

        mockMvc.perform(put("/api/admin/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật sản phẩm thành công"));
    }

    @Test
    @DisplayName("DELETE /api/admin/products/{id} — Admin xóa sản phẩm")
    void adminDeleteProduct_success() throws Exception {
        doNothing().when(deleteProductUseCase).execute(productId);

        mockMvc.perform(delete("/api/admin/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa sản phẩm"));
    }

    @Test
    @DisplayName("POST /api/admin/products/{id}/variants — Thêm variant mới")
    void adminAddVariant_success() throws Exception {
        when(manageVariantUseCase.add(eq(productId), any())).thenReturn(product);

        Map<String, Object> reqBody = Map.of(
                "sku", "JEAN-02",
                "size", "31",
                "color", "Đen",
                "price", 450000,
                "stock", 10
        );

        mockMvc.perform(post("/api/admin/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
