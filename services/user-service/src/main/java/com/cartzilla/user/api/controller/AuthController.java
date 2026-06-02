package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.dto.AuthDtos.*;
import com.cartzilla.user.application.usecase.LoginUseCase;
import com.cartzilla.user.application.usecase.RegisterUserUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UUID>> register(@Valid @RequestBody RegisterRequest req) {
        UUID id = registerUserUseCase.execute(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đăng ký thành công", id));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var r = loginUseCase.execute(req.toCommand());
        return ApiResponse.ok(new LoginResponse(r.accessToken(), r.email(), r.role()));
    }
}
