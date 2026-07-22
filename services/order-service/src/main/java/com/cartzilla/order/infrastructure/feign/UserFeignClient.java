package com.cartzilla.order.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
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
    ApiResponse<UserDto> getUserById(@PathVariable UUID userId);

    /**
     * Lấy default address của user — dùng tại checkout nếu address không được truyền tường minh.
     * UA-06: user phải có ít nhất 1 address.
     */
    @GetMapping("/api/internal/users/{userId}/default-address")
    ApiResponse<AddressDto> getDefaultAddress(@PathVariable UUID userId);

    /**
     * Validate + preview discount cho voucher — VA-07, VA-09, BR-V09.
     * Không tăng usedCount.
     */
    @GetMapping("/api/internal/vouchers/{code}/validate")
    ApiResponse<VoucherValidationDto> validateVoucher(@PathVariable String code,
                                                        @org.springframework.web.bind.annotation.RequestParam UUID userId,
                                                        @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal orderSubtotal);

    @PostMapping("/api/internal/vouchers/redeem")
    ApiResponse<VoucherRedeemDto> redeemVoucher(@RequestBody VoucherRedeemRequest request);

    @PostMapping("/api/internal/vouchers/release")
    ApiResponse<Boolean> releaseVoucher(@RequestBody VoucherReleaseRequest request);

    // ─── Inner DTOs (response payload từ user-service) ───────────────────────

    record UserDto(UUID id, String email, String fullName, boolean active, String role) {}

    record AddressDto(UUID id, String fullName, String phone,
                       String street, String district, String city) {}

    record VoucherValidationDto(String code, java.math.BigDecimal discountAmount,
                                 String discountType, boolean valid, String message) {}

    record VoucherRedeemRequest(String code, UUID userId, UUID orderId, BigDecimal orderSubtotal) {}

    record VoucherRedeemDto(UUID usageId, String code, BigDecimal discountAmount, boolean idempotent) {}

    record VoucherReleaseRequest(String code, UUID userId, UUID orderId) {}
}
