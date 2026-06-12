package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.VoucherDtos.AddAllowedUserRequest;
import com.cartzilla.user.api.dto.VoucherDtos.AllowedUserResponse;
import com.cartzilla.user.api.dto.VoucherDtos.CreateVoucherRequest;
import com.cartzilla.user.api.dto.VoucherDtos.UpdateVoucherRequest;
import com.cartzilla.user.api.dto.VoucherDtos.VoucherResponse;
import com.cartzilla.user.application.usecase.AddVoucherAllowedUserUseCase;
import com.cartzilla.user.application.usecase.CreateVoucherUseCase;
import com.cartzilla.user.application.usecase.DeleteVoucherUseCase;
import com.cartzilla.user.application.usecase.GetVoucherUseCase;
import com.cartzilla.user.application.usecase.ListVouchersUseCase;
import com.cartzilla.user.application.usecase.RemoveVoucherAllowedUserUseCase;
import com.cartzilla.user.application.usecase.UpdateVoucherUseCase;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ADMIN_VOUCHERS)
@RequiredArgsConstructor
public class AdminVoucherController {

    private final ListVouchersUseCase listVouchersUseCase;
    private final GetVoucherUseCase getVoucherUseCase;
    private final CreateVoucherUseCase createVoucherUseCase;
    private final UpdateVoucherUseCase updateVoucherUseCase;
    private final DeleteVoucherUseCase deleteVoucherUseCase;
    private final AddVoucherAllowedUserUseCase addVoucherAllowedUserUseCase;
    private final RemoveVoucherAllowedUserUseCase removeVoucherAllowedUserUseCase;

    @GetMapping
    public ApiResponse<List<VoucherResponse>> list(@RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ApiResponse.ok(listVouchersUseCase.execute().stream()
                .map(VoucherResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> get(@RequestHeader("X-User-Role") String role,
                                            @PathVariable UUID id) {
        requireAdmin(role);
        return ApiResponse.ok(VoucherResponse.from(getVoucherUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> create(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateVoucherRequest request) {
        requireAdmin(role);
        var voucher = createVoucherUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Voucher created", VoucherResponse.from(voucher)));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> update(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        requireAdmin(role);
        return ApiResponse.ok("Voucher updated", VoucherResponse.from(
                updateVoucherUseCase.execute(id, request.toCommand())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Role") String role,
                                    @PathVariable UUID id) {
        requireAdmin(role);
        deleteVoucherUseCase.execute(id);
        return ApiResponse.ok("Voucher deleted", null);
    }

    @PostMapping("/{id}/allowed-users")
    public ResponseEntity<ApiResponse<AllowedUserResponse>> addAllowedUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @Valid @RequestBody AddAllowedUserRequest request) {
        requireAdmin(role);
        var allowedUser = addVoucherAllowedUserUseCase.execute(id, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Allowed user added", AllowedUserResponse.from(allowedUser)));
    }

    @DeleteMapping("/{id}/allowed-users/{userId}")
    public ApiResponse<Void> removeAllowedUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        requireAdmin(role);
        removeVoucherAllowedUserUseCase.execute(id, userId);
        return ApiResponse.ok("Allowed user removed", null);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException("Admin role required");
        }
    }
}
