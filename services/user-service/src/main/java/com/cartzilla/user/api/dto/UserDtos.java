package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserDtos {
    private UserDtos() {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 20) String phone) {
        public UserCommand.UpdateProfile toCommand() {
            return new UserCommand.UpdateProfile(fullName, phone);
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6) String newPassword) {
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
