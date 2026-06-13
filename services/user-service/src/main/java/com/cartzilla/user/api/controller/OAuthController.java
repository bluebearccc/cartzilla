package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.dto.OAuthDtos.AuthorizationResponse;
import com.cartzilla.user.api.dto.OAuthDtos.OAuthLoginResponse;
import com.cartzilla.user.application.usecase.CompleteOAuthLoginUseCase;
import com.cartzilla.user.application.usecase.StartOAuthLoginUseCase;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {
    private final StartOAuthLoginUseCase startOAuthLoginUseCase;
    private final CompleteOAuthLoginUseCase completeOAuthLoginUseCase;

    @GetMapping("/{provider}/authorize")
    public ApiResponse<AuthorizationResponse> authorize(@PathVariable String provider) {
        var result = startOAuthLoginUseCase.execute(parseProvider(provider));
        return ApiResponse.ok(new AuthorizationResponse(result.authorizationUrl(), result.state()));
    }

    @GetMapping("/{provider}/callback")
    public ApiResponse<OAuthLoginResponse> callback(
            @PathVariable String provider,
            @RequestParam String code) {
        var result = completeOAuthLoginUseCase.execute(parseProvider(provider), code);
        return ApiResponse.ok(new OAuthLoginResponse(
                result.accessToken(), result.refreshToken(), result.email(), result.role()));
    }

    private OAuthProvider parseProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unsupported OAuth provider: " + provider);
        }
    }
}
