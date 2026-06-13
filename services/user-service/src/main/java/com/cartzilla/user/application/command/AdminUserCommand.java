package com.cartzilla.user.application.command;

import com.cartzilla.user.domain.vo.Role;

public class AdminUserCommand {
    private AdminUserCommand() {}

    public record Search(String keyword, Role role, Boolean active) {}

    public record UpdateRole(Role role) {}

    public record UpdateStatus(boolean active) {}
}
