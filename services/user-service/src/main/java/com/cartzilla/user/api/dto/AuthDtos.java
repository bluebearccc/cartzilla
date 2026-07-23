package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.AuthCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).*$", message = "Password must contain at least one letter and one number") String password,
            @NotBlank String fullName) {
        public AuthCommand.Register toCommand() {
            return new AuthCommand.Register(email, password, fullName);
        }
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
        public AuthCommand.Login toCommand() {
            return new AuthCommand.Login(email, password);
        }
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).*$", message = "Password must contain at least one letter and one number") String newPassword) {}

    public record VerifyEmailRequest(@NotBlank String token) {}

    public record ResendVerificationRequest(@Email @NotBlank String email) {}

    public record LoginResponse(String accessToken, String refreshToken, String email, String role) {}
}
