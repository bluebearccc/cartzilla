package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.AddressDtos.AddressResponse;
import com.cartzilla.user.api.dto.AddressDtos.CreateAddressRequest;
import com.cartzilla.user.api.dto.AddressDtos.UpdateAddressRequest;
import com.cartzilla.user.application.usecase.CreateAddressUseCase;
import com.cartzilla.user.application.usecase.DeleteAddressUseCase;
import com.cartzilla.user.application.usecase.ListAddressesUseCase;
import com.cartzilla.user.application.usecase.SetDefaultAddressUseCase;
import com.cartzilla.user.application.usecase.UpdateAddressUseCase;
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
@RequestMapping(ApiPaths.USER_ADDRESSES)
@RequiredArgsConstructor
public class AddressController {

    private final ListAddressesUseCase listAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(listAddressesUseCase.execute(userId).stream()
                .map(AddressResponse::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateAddressRequest request) {
        var address = createAddressUseCase.execute(userId, request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address created", AddressResponse.from(address)));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ApiResponse.ok("Address updated", AddressResponse.from(
                updateAddressUseCase.execute(userId, id, request.toCommand())));
    }

    @PutMapping("/{id}/default")
    public ApiResponse<AddressResponse> setDefault(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.ok("Default address updated", AddressResponse.from(
                setDefaultAddressUseCase.execute(userId, id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        deleteAddressUseCase.execute(userId, id);
        return ApiResponse.ok("Address deleted", null);
    }
}
