package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cartzilla.product.application.usecase.CreateVendorUseCase;
import com.cartzilla.product.application.usecase.DeleteVendorUseCase;
import com.cartzilla.product.application.usecase.ListVendorsUseCase;
import com.cartzilla.product.application.usecase.UpdateVendorUseCase;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.vo.VendorType;
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
class VendorControllersTest {

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ListVendorsUseCase listVendorsUseCase;
    @Mock
    private CreateVendorUseCase createVendorUseCase;
    @Mock
    private UpdateVendorUseCase updateVendorUseCase;
    @Mock
    private DeleteVendorUseCase deleteVendorUseCase;

    @InjectMocks
    private VendorController vendorController;

    @InjectMocks
    private AdminVendorController adminVendorController;

    private Vendor vendor;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        publicMockMvc = MockMvcBuilders.standaloneSetup(vendorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminMockMvc = MockMvcBuilders.standaloneSetup(adminVendorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        vendorId = UUID.randomUUID();
        vendor = Vendor.create("Nike", "nike", VendorType.BRAND, "contact@nike.com", "0900000000", "https://nike.com", "https://logo.png");
    }

    @Test
    @DisplayName("GET /api/vendors — Public Lấy danh sách vendor active")
    void publicList_success() throws Exception {
        when(listVendorsUseCase.execute(false)).thenReturn(List.of(vendor));

        publicMockMvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Nike"));
    }

    @Test
    @DisplayName("POST /api/admin/vendors — Admin tạo vendor")
    void adminCreate_success() throws Exception {
        when(createVendorUseCase.execute(any())).thenReturn(vendor);

        Map<String, Object> reqBody = Map.of(
                "name", "Nike",
                "slug", "nike",
                "vendorType", "BRAND",
                "email", "contact@nike.com"
        );

        adminMockMvc.perform(post("/api/admin/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo vendor thành công"));
    }

    @Test
    @DisplayName("PUT /api/admin/vendors/{id} — Admin cập nhật vendor")
    void adminUpdate_success() throws Exception {
        when(updateVendorUseCase.execute(eq(vendorId), any())).thenReturn(vendor);

        Map<String, Object> reqBody = Map.of(
                "name", "Nike Official",
                "slug", "nike-official",
                "vendorType", "BRAND",
                "active", true
        );

        adminMockMvc.perform(put("/api/admin/vendors/" + vendorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật vendor thành công"));
    }

    @Test
    @DisplayName("DELETE /api/admin/vendors/{id} — Admin xóa vendor")
    void adminDelete_success() throws Exception {
        doNothing().when(deleteVendorUseCase).execute(vendorId);

        adminMockMvc.perform(delete("/api/admin/vendors/" + vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa vendor"));
    }
}
