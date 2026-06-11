package com.cartzilla.order.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client gọi user-service.
 * Dùng Eureka service name: "user-service".
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserFeignClient {

    /**
     * Lấy thông tin user để kiểm tra isActive (UA-04, CTA-04).
     * Dùng trước khi thao tác cart / checkout.
     */
    @GetMapping("/api/internal/users/{userId}")
    ApiResponse<UserDto> getUserById(@PathVariable("userId") UUID userId);

    /**
     * Lấy default address của user — dùng tại checkout nếu address không được truyền tường minh.
     * UA-06: user phải có ít nhất 1 address.
     */
    @GetMapping("/api/internal/users/{userId}/default-address")
    ApiResponse<AddressDto> getDefaultAddress(@PathVariable("userId") UUID userId);

    /**
     * Validate + preview discount cho voucher — VA-07, VA-09, BR-V09.
     * Không tăng usedCount.
     */
    @GetMapping("/api/internal/vouchers/{code}/validate")
    ApiResponse<VoucherValidationDto> validateVoucher(@PathVariable("code") String code,
                                                        @org.springframework.web.bind.annotation.RequestParam UUID userId,
                                                        @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal orderSubtotal);

    // ─── Inner DTOs (response payload từ user-service) ───────────────────────

    record UserDto(UUID id, String email, String fullName, boolean active, String role) {}

    record AddressDto(UUID id, String fullName, String phone,
                       String street, String district, String city) {}

    record VoucherValidationDto(String code, java.math.BigDecimal discountAmount,
                                 String discountType, boolean valid, String message) {}
}
