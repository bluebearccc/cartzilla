package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.dto.OAuthDtos.AuthorizationResponse;
import com.cartzilla.user.api.dto.OAuthDtos.OAuthLoginResponse;
import com.cartzilla.user.application.usecase.CompleteOAuthLoginUseCase;
import com.cartzilla.user.application.usecase.StartOAuthLoginUseCase;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {
    private final StartOAuthLoginUseCase startOAuthLoginUseCase;
    private final CompleteOAuthLoginUseCase completeOAuthLoginUseCase;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/{provider}/authorize")
    public ApiResponse<AuthorizationResponse> authorize(@PathVariable String provider) {
        var result = startOAuthLoginUseCase.execute(parseProvider(provider));
        return ApiResponse.ok(new AuthorizationResponse(result.authorizationUrl(), result.state()));
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<?> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @RequestHeader(value = "Accept", defaultValue = "") String acceptHeader) {

        boolean isBrowser = acceptHeader.contains("text/html");

        try {
            var result = completeOAuthLoginUseCase.execute(parseProvider(provider), code, state);

            if (isBrowser) {
                String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                        .queryParam("accessToken", result.accessToken())
                        .queryParam("refreshToken", result.refreshToken())
                        .queryParam("email", result.email())
                        .queryParam("role", result.role())
                        .build().toUriString();
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", redirectUrl)
                        .build();
            }

            return ResponseEntity.ok(ApiResponse.ok(new OAuthLoginResponse(
                    result.accessToken(), result.refreshToken(), result.email(), result.role())));

        } catch (Exception ex) {
            if (isBrowser) {
                // Redirect về /login?error=... thay vì trả raw JSON
                String errorMsg = ex.getMessage() != null ? ex.getMessage() : "Đăng nhập Google thất bại";
                String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                        .queryParam("error", errorMsg)
                        .build().toUriString();
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", redirectUrl)
                        .build();
            }
            // AJAX caller vẫn nhận JSON lỗi bình thường
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    private OAuthProvider parseProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unsupported OAuth provider: " + provider);
        }
    }
}
