package com.cartzilla.user.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${clients.notification-service.url:}", configuration = FeignConfig.class)
public interface NotificationFeignClient {
    @PostMapping("/api/internal/notifications/reset-password-email")
    ApiResponse<Void> sendResetPasswordEmail(@RequestBody ResetPasswordEmailRequest request);

    @PostMapping("/api/internal/notifications/verification-email")
    ApiResponse<Void> sendVerificationEmail(@RequestBody VerificationEmailRequest request);

    record ResetPasswordEmailRequest(String email, String fullName, String resetLink, int expiresInMinutes) {}
    record VerificationEmailRequest(String email, String fullName, String verificationLink, int expiresInMinutes) {}
}
