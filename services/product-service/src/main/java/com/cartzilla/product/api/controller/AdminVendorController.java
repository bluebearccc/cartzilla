package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.VendorDtos.CreateVendorRequest;
import com.cartzilla.product.api.dto.VendorDtos.UpdateVendorRequest;
import com.cartzilla.product.api.dto.VendorDtos.VendorResponse;
import com.cartzilla.product.application.usecase.CreateVendorUseCase;
import com.cartzilla.product.application.usecase.DeleteVendorUseCase;
import com.cartzilla.product.application.usecase.ListVendorsUseCase;
import com.cartzilla.product.application.usecase.UpdateVendorUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Admin vendor — F16 (UC-05): CRUD/deactivate vendor. */
@RestController
@RequestMapping(ApiPaths.ADMIN_VENDORS)
@RequiredArgsConstructor
public class AdminVendorController {

    private final ListVendorsUseCase listVendorsUseCase;
    private final CreateVendorUseCase createVendorUseCase;
    private final UpdateVendorUseCase updateVendorUseCase;
    private final DeleteVendorUseCase deleteVendorUseCase;

    /** GET /api/admin/vendors — gồm cả inactive */
    @GetMapping
    public ApiResponse<List<VendorResponse>> list() {
        return ApiResponse.ok(listVendorsUseCase.execute(true).stream()
                .map(VendorResponse::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> create(
            @Valid @RequestBody CreateVendorRequest req) {
        var vendor = createVendorUseCase.execute(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo vendor thành công", VendorResponse.from(vendor)));
    }

    @PutMapping("/{id}")
    public ApiResponse<VendorResponse> update(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateVendorRequest req) {
        var vendor = updateVendorUseCase.execute(id, req.toCommand());
        return ApiResponse.ok("Cập nhật vendor thành công", VendorResponse.from(vendor));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        deleteVendorUseCase.execute(id);
        return ApiResponse.ok("Đã xóa vendor", null);
    }
}
