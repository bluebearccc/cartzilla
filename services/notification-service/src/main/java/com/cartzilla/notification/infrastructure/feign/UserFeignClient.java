package com.cartzilla.notification.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client gọi user-service từ notification-service.
 * Dùng để lấy email khi event không chứa sẵn — NA-04.
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserFeignClient {

    /** Lấy email + fullName của user để gửi email notification. */
    @GetMapping("/api/internal/users/{userId}/contact")
    ApiResponse<UserContactDto> getUserContact(@PathVariable UUID userId);

    record UserContactDto(UUID id, String email, String fullName) {}
}
