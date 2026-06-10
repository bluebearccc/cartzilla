package com.cartzilla.user.api.controller;

import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.infrastructure.persistence.VoucherJpaRepository;
import com.cartzilla.user.infrastructure.persistence.VoucherUsageJpaRepository;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Internal voucher validation endpoint — gọi từ order-service qua Feign.
 * VA-07: validate chỉ preview, KHÔNG tăng usedCount.
 */
@RestController
@RequestMapping("/api/internal/vouchers")
@RequiredArgsConstructor
public class InternalVoucherController {

    private final VoucherJpaRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherUsageJpaRepository usageRepository;

    /**
     * Validate voucher + tính discount preview.
     * Rules: VA-03..VA-09, V-01..V-14, BR-V02..BR-V14.
     */
    @GetMapping("/{code}/validate")
    public ApiResponse<VoucherValidationDto> validate(
            @PathVariable String code,
            @RequestParam UUID userId,
            @RequestParam BigDecimal orderSubtotal) {

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + code));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));

        // UA-04
        user.requireActive();

        Instant now = Instant.now();

        // VA-03
        if (!voucher.isRedeemable(now))
            throw new BusinessException("Voucher is not redeemable (VA-03)");

        // V-04: min order amount
        if (orderSubtotal.compareTo(voucher.getMinOrderAmount()) < 0)
            throw new BusinessException(
                    "Order subtotal " + orderSubtotal + " < minOrderAmount " + voucher.getMinOrderAmount() + " (V-04)");

        // VA-06/V-09: account age
        if (voucher.getMinAccountAgeDays() > 0) {
            long ageDays = ChronoUnit.DAYS.between(
                    user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    now.atZone(ZoneOffset.UTC).toLocalDate());
            if (ageDays < voucher.getMinAccountAgeDays())
                throw new BusinessException(
                        "Voucher requires account age of at least " + voucher.getMinAccountAgeDays() +
                                " days (VA-06). Current: " + ageDays + " days");
        }

        // V-11/BR-V09: per user limit
        long userUsageCount = usageRepository.countByVoucherIdAndUserId(voucher.getId(), userId);
        if (userUsageCount >= voucher.getPerUserLimit())
            throw new BusinessException(
                    "User has reached perUserLimit=" + voucher.getPerUserLimit() + " for this voucher (V-11)");

        // Tính discount — V-02/V-03
        BigDecimal discount = calculateDiscount(voucher, orderSubtotal);

        return ApiResponse.ok(new VoucherValidationDto(
                voucher.getCode(), discount,
                voucher.getDiscountType().name(), true, "Voucher valid"));
    }

    private BigDecimal calculateDiscount(Voucher v, BigDecimal subtotal) {
        return switch (v.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal raw = subtotal.multiply(v.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
                // V-02: cap at maxDiscountAmount
                yield v.getMaxDiscountAmount() != null && raw.compareTo(v.getMaxDiscountAmount()) > 0
                        ? v.getMaxDiscountAmount() : raw;
            }
            case FIXED_AMOUNT -> v.getDiscountValue().min(subtotal);
        };
    }

    record VoucherValidationDto(String code, BigDecimal discountAmount,
                                 String discountType, boolean valid, String message) {}
}
