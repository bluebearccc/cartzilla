package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.VendorDtos.VendorResponse;
import com.cartzilla.product.application.usecase.ListVendorsUseCase;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public vendor list — UC-01 (filter theo vendor/brand). */
@RestController
@RequestMapping(ApiPaths.VENDORS)
@RequiredArgsConstructor
public class VendorController {

    private final ListVendorsUseCase listVendorsUseCase;

    /** GET /api/vendors — vendor active cho filter dropdown */
    @GetMapping
    public ApiResponse<List<VendorResponse>> list() {
        return ApiResponse.ok(listVendorsUseCase.execute(false).stream()
                .map(VendorResponse::from)
                .toList());
    }
}
