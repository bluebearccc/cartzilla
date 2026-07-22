package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.VoucherDtos.RedeemVoucherRequest;
import com.cartzilla.user.api.dto.VoucherDtos.VoucherRedeemResponse;
import com.cartzilla.user.api.dto.VoucherDtos.VoucherValidationResponse;
import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.application.usecase.RedeemVoucherUseCase;
import com.cartzilla.user.application.usecase.ValidateVoucherUseCase;
import com.cartzilla.user.application.usecase.ReleaseVoucherUseCase;
import com.cartzilla.user.api.dto.VoucherDtos.ReleaseVoucherRequest;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.INTERNAL_VOUCHERS)
@RequiredArgsConstructor
public class InternalVoucherController {

    private final ValidateVoucherUseCase validateVoucherUseCase;
    private final RedeemVoucherUseCase redeemVoucherUseCase;
    private final ReleaseVoucherUseCase releaseVoucherUseCase;

    @GetMapping("/{code}/validate")
    public ApiResponse<VoucherValidationResponse> validate(
            @PathVariable String code,
            @RequestParam UUID userId,
            @RequestParam BigDecimal orderSubtotal) {
        return ApiResponse.ok(VoucherValidationResponse.from(validateVoucherUseCase.execute(
                new VoucherCommand.Validate(code, userId, orderSubtotal))));
    }

    @PostMapping("/redeem")
    public ApiResponse<VoucherRedeemResponse> redeem(@Valid @RequestBody RedeemVoucherRequest request) {
        return ApiResponse.ok(VoucherRedeemResponse.from(redeemVoucherUseCase.execute(request.toCommand())));
    }

    @PostMapping("/release")
    public ApiResponse<Boolean> release(@Valid @RequestBody ReleaseVoucherRequest request) {
        return ApiResponse.ok(releaseVoucherUseCase.execute(request.code(), request.userId(), request.orderId()));
    }
}
