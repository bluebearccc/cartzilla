package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.vo.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AdminUserDtos {
    private AdminUserDtos() {}

    public record UpdateRoleRequest(@NotNull Role role) {
        public AdminUserCommand.UpdateRole toCommand() {
            return new AdminUserCommand.UpdateRole(role);
        }
    }

    public record UpdateStatusRequest(boolean active) {
        public AdminUserCommand.UpdateStatus toCommand() {
            return new AdminUserCommand.UpdateStatus(active);
        }
    }

    public record UserResponse(
            UUID id,
            String email,
            String fullName,
            String phone,
            Role role,
            boolean emailVerified,
            boolean active) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getRole(),
                    user.isEmailVerified(),
                    user.isActive());
        }
    }
}
