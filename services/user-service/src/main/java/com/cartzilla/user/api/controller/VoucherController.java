package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.VoucherDtos.ValidateVoucherRequest;
import com.cartzilla.user.api.dto.VoucherDtos.VoucherValidationResponse;
import com.cartzilla.user.application.usecase.ValidateVoucherUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.VOUCHERS)
@RequiredArgsConstructor
public class VoucherController {

    private final ValidateVoucherUseCase validateVoucherUseCase;

    @PostMapping("/validate")
    public ApiResponse<VoucherValidationResponse> validate(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ValidateVoucherRequest request) {
        return ApiResponse.ok(VoucherValidationResponse.from(
                validateVoucherUseCase.execute(request.toCommand(userId))));
    }
}
