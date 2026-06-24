package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class UserDtos {
    private UserDtos() {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = "^$|^0\\d{9}$", message = "Phone number must be exactly 10 digits starting with 0")
            @Size(max = 20) String phone) {
        public UserCommand.UpdateProfile toCommand() {
            return new UserCommand.UpdateProfile(fullName, phone);
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).*$", message = "Password must contain at least one letter and one number") String newPassword) {
        public UserCommand.ChangePassword toCommand() {
            return new UserCommand.ChangePassword(currentPassword, newPassword);
        }
    }

    public record ProfileResponse(
            UUID id,
            String email,
            String fullName,
            String phone,
            String role,
            boolean emailVerified,
            boolean active) {
        public static ProfileResponse from(User user) {
            return new ProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getRole().name(),
                    user.isEmailVerified(),
                    user.isActive());
        }
    }
}
