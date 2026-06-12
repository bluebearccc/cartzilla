package com.cartzilla.user.application.command;

import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class VoucherCommand {
    private VoucherCommand() {}

    public record Create(
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            int maxUses,
            Instant startsAt,
            Instant expiresAt,
            int minAccountAgeDays,
            int perUserLimit,
            VoucherAudienceType audienceType,
            boolean firstOrderOnly,
            int minCompletedOrders,
            BigDecimal minTotalSpent) {}

    public record Update(
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            int maxUses,
            Instant startsAt,
            Instant expiresAt,
            int minAccountAgeDays,
            int perUserLimit,
            VoucherAudienceType audienceType,
            boolean firstOrderOnly,
            int minCompletedOrders,
            BigDecimal minTotalSpent,
            Boolean active) {}

    public record Validate(String code, UUID userId, BigDecimal orderSubtotal) {}

    public record Redeem(String code, UUID userId, UUID orderId, BigDecimal orderSubtotal) {}
}
