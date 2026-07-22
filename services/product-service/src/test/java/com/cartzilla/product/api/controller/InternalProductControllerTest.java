package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cartzilla.product.application.usecase.GetVariantSnapshotUseCase;
import com.cartzilla.product.application.usecase.GetVariantSnapshotUseCase.VariantSnapshot;
import com.cartzilla.product.application.usecase.ReserveStockUseCase;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GetVariantSnapshotUseCase getVariantSnapshotUseCase;

    @Mock
    private ReserveStockUseCase reserveStockUseCase;

    @InjectMocks
    private InternalProductController internalProductController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalProductController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/internal/products/variants/{sku} — Lấy Variant Snapshot cho Feign")
    void getVariantBySku_success() throws Exception {
        VariantSnapshot snapshot = new VariantSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "SKU-001", "Áo thun", "https://img.jpg", "M", "Red",
                new BigDecimal("199000.00"), 10, true
        );
        when(getVariantSnapshotUseCase.execute("SKU-001")).thenReturn(snapshot);

        mockMvc.perform(get("/api/internal/products/variants/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"))
                .andExpect(jsonPath("$.data.productName").value("Áo thun"));
    }

    @Test
    @DisplayName("PUT /api/internal/products/variants/reserve — Giữ kho cho danh sách sản phẩm")
    void reserveStock_success() throws Exception {
        doNothing().when(reserveStockUseCase).reserveOrThrow(anyList());

        List<Map<String, Object>> requests = List.of(
                Map.of("sku", "SKU-001", "quantity", 2),
                Map.of("sku", "SKU-002", "quantity", 1)
        );

        mockMvc.perform(put("/api/internal/products/variants/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /api/internal/products/variants/release — Hoàn kho khi hủy đơn")
    void releaseStock_success() throws Exception {
        doNothing().when(reserveStockUseCase).releaseLines(anyList());

        List<Map<String, Object>> requests = List.of(
                Map.of("sku", "SKU-001", "quantity", 2)
        );

        mockMvc.perform(put("/api/internal/products/variants/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
