package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.AuthDtos.ForgotPasswordRequest;
import com.cartzilla.user.api.dto.AuthDtos.LoginRequest;
import com.cartzilla.user.api.dto.AuthDtos.LoginResponse;
import com.cartzilla.user.api.dto.AuthDtos.LogoutRequest;
import com.cartzilla.user.api.dto.AuthDtos.RefreshTokenRequest;
import com.cartzilla.user.api.dto.AuthDtos.RegisterRequest;
import com.cartzilla.user.api.dto.AuthDtos.ResetPasswordRequest;
import com.cartzilla.user.api.dto.AuthDtos.VerifyEmailRequest;
import com.cartzilla.user.application.usecase.ForgotPasswordUseCase;
import com.cartzilla.user.application.usecase.LoginUseCase;
import com.cartzilla.user.application.usecase.LogoutUseCase;
import com.cartzilla.user.application.usecase.RefreshTokenUseCase;
import com.cartzilla.user.application.usecase.RegisterUserUseCase;
import com.cartzilla.user.application.usecase.ResetPasswordUseCase;
import com.cartzilla.user.application.usecase.VerifyEmailUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.USERS)
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UUID>> register(@Valid @RequestBody RegisterRequest req) {
        UUID id = registerUserUseCase.execute(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. Please verify your email.", id));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var r = loginUseCase.execute(req.toCommand());
        return ApiResponse.ok(new LoginResponse(r.accessToken(), r.refreshToken(), r.email(), r.role()));
    }

    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        var r = refreshTokenUseCase.execute(req.refreshToken());
        return ApiResponse.ok(new LoginResponse(r.accessToken(), r.refreshToken(), r.email(), r.role()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest req) {
        logoutUseCase.execute(req.refreshToken());
        return ApiResponse.ok("Logged out", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        forgotPasswordUseCase.execute(req.email());
        return ApiResponse.ok("Password reset email queued", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        resetPasswordUseCase.execute(req.token(), req.newPassword());
        return ApiResponse.ok("Password reset successful", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        verifyEmailUseCase.execute(req.token());
        return ApiResponse.ok("Email verified", null);
    }
}
