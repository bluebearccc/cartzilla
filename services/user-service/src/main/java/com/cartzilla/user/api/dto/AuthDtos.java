package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.AuthCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/Response DTO cho auth (kèm map sang command). */
public class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password,
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

    public record LoginResponse(String accessToken, String email, String role) {}
}
