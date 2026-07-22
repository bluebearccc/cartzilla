package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cartzilla.product.application.usecase.GetProductUseCase;
import com.cartzilla.product.application.usecase.ListProductsUseCase;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ListProductsUseCase listProductsUseCase;

    @Mock
    private GetProductUseCase getProductUseCase;

    @InjectMocks
    private ProductController productController;

    private Product sampleProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        productId = UUID.randomUUID();
        sampleProduct = Product.create(UUID.randomUUID(), "Áo Sơ Mi", "ao-so-mi", "Sơ mi lụa", new BigDecimal("350000"), null, "so-mi");
        sampleProduct.addVariant(ProductVariant.create("SOM-01", "M", "Trắng", "#FFFFFF", new BigDecimal("350000"), 10));
        sampleProduct.addImage(ProductImage.create("https://img/somi.jpg", "Áo sơ mi", true, 0));
    }

    @Test
    @DisplayName("GET /api/products — Lấy danh sách sản phẩm public")
    void listProducts_success() throws Exception {
        when(listProductsUseCase.execute(any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sampleProduct)));

        mockMvc.perform(get("/api/products")
                        .param("q", "Sơ Mi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Áo Sơ Mi"))
                .andExpect(jsonPath("$.data.items[0].slug").value("ao-so-mi"));
    }

    @Test
    @DisplayName("GET /api/products/{id} — Lấy chi tiết sản phẩm theo ID")
    void getProductDetail_success() throws Exception {
        when(getProductUseCase.execute(productId)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Áo Sơ Mi"))
                .andExpect(jsonPath("$.data.variants[0].sku").value("SOM-01"));
    }

    @Test
    @DisplayName("GET /api/products/slug/{slug} — Lấy chi tiết sản phẩm theo Slug")
    void getProductDetailBySlug_success() throws Exception {
        when(getProductUseCase.executeBySlug("ao-so-mi")).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/products/slug/ao-so-mi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("ao-so-mi"));
    }
}
