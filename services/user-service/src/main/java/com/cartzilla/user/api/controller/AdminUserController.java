package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.AdminUserDtos.UpdateRoleRequest;
import com.cartzilla.user.api.dto.AdminUserDtos.UpdateStatusRequest;
import com.cartzilla.user.api.dto.AdminUserDtos.UserResponse;
import com.cartzilla.user.api.dto.PageResponse;
import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.application.usecase.EnsureAdminUseCase;
import com.cartzilla.user.application.usecase.GetUserUseCase;
import com.cartzilla.user.application.usecase.ListUsersUseCase;
import com.cartzilla.user.application.usecase.UpdateUserRoleUseCase;
import com.cartzilla.user.application.usecase.UpdateUserStatusUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ADMIN_USERS)
@RequiredArgsConstructor
public class AdminUserController {

    private final EnsureAdminUseCase ensureAdminUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserRoleUseCase updateUserRoleUseCase;
    private final UpdateUserStatusUseCase updateUserStatusUseCase;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestHeader("X-User-Id") UUID adminUserId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.cartzilla.user.domain.vo.Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "email,asc") String sort) {
        ensureAdminUseCase.execute(adminUserId);
        Page<UserResponse> result = listUsersUseCase.execute(
                new AdminUserCommand.Search(q, role, active), page, limit, sort).map(UserResponse::from);
        return ApiResponse.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@RequestHeader("X-User-Id") UUID adminUserId,
                                         @PathVariable UUID id) {
        ensureAdminUseCase.execute(adminUserId);
        return ApiResponse.ok(UserResponse.from(getUserUseCase.execute(id)));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<UserResponse> updateRole(@RequestHeader("X-User-Id") UUID adminUserId,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody UpdateRoleRequest request) {
        ensureAdminUseCase.execute(adminUserId);
        return ApiResponse.ok("User role updated", UserResponse.from(
                updateUserRoleUseCase.execute(id, request.toCommand())));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(@RequestHeader("X-User-Id") UUID adminUserId,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody UpdateStatusRequest request) {
        ensureAdminUseCase.execute(adminUserId);
        return ApiResponse.ok("User status updated", UserResponse.from(
                updateUserStatusUseCase.execute(id, request.toCommand())));
    }
}
