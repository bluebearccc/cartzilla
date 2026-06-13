package com.cartzilla.notification.api.controller;

import com.cartzilla.notification.application.usecase.SendResetPasswordEmailUseCase;
import com.cartzilla.notification.application.usecase.SendVerificationEmailUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {
    private final SendResetPasswordEmailUseCase sendResetPasswordEmailUseCase;
    private final SendVerificationEmailUseCase sendVerificationEmailUseCase;

    @PostMapping("/reset-password-email")
    public ApiResponse<Void> sendResetPasswordEmail(@Valid @RequestBody ResetPasswordEmailRequest request) {
        sendResetPasswordEmailUseCase.execute(
                request.email(), request.fullName(), request.resetLink(), request.expiresInMinutes());
        return ApiResponse.ok("Reset password email processed", null);
    }

    @PostMapping("/verification-email")
    public ApiResponse<Void> sendVerificationEmail(@Valid @RequestBody VerificationEmailRequest request) {
        sendVerificationEmailUseCase.execute(
                request.email(), request.fullName(), request.verificationLink(), request.expiresInMinutes());
        return ApiResponse.ok("Verification email processed", null);
    }

    public record ResetPasswordEmailRequest(
            @Email @NotBlank String email,
            String fullName,
            @NotBlank String resetLink,
            @Min(1) int expiresInMinutes) {}

    public record VerificationEmailRequest(
            @Email @NotBlank String email,
            String fullName,
            @NotBlank String verificationLink,
            @Min(1) int expiresInMinutes) {}
}
