package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.api.dto.UserDtos.ChangeEmailRequest;
import com.cartzilla.user.api.dto.UserDtos.ChangePasswordRequest;
import com.cartzilla.user.api.dto.UserDtos.ProfileResponse;
import com.cartzilla.user.api.dto.UserDtos.UpdateProfileRequest;
import com.cartzilla.user.application.usecase.ChangeEmailUseCase;
import com.cartzilla.user.application.usecase.ChangePasswordUseCase;
import com.cartzilla.user.application.usecase.GetProfileUseCase;
import com.cartzilla.user.application.usecase.UpdateProfileUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.USER_ME)
@RequiredArgsConstructor
public class ProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangeEmailUseCase changeEmailUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(ProfileResponse.from(getProfileUseCase.execute(userId)));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated", ProfileResponse.from(
                updateProfileUseCase.execute(userId, request.toCommand())));
    }

    @PutMapping("/email")
    public ApiResponse<ProfileResponse> changeEmail(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ChangeEmailRequest request) {
        return ApiResponse.ok("Email changed", ProfileResponse.from(
                changeEmailUseCase.execute(userId, request.toCommand())));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.execute(userId, request.toCommand());
        return ApiResponse.ok("Password changed", null);
    }
}
