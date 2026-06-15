package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.application.usecase.RedeemVoucherUseCase;
import com.cartzilla.user.application.usecase.ValidateVoucherUseCase;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.entity.VoucherAllowedUser;
import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class VoucherDtos {
    private VoucherDtos() {}

    public record CreateVoucherRequest(
            @NotBlank @Size(max = 50) String code,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin(value = "0.01") BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            @Min(1) int maxUses,
            Instant startsAt,
            Instant expiresAt,
            @Min(0) int minAccountAgeDays,
            @Min(1) int perUserLimit,
            VoucherAudienceType audienceType,
            boolean firstOrderOnly,
            @Min(0) int minCompletedOrders,
            BigDecimal minTotalSpent) {
        public VoucherCommand.Create toCommand() {
            return new VoucherCommand.Create(code, discountType, discountValue, maxDiscountAmount,
                    minOrderAmount, maxUses, startsAt, expiresAt, minAccountAgeDays,
                    perUserLimit, audienceType, firstOrderOnly, minCompletedOrders, minTotalSpent);
        }
    }

    public record UpdateVoucherRequest(
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin(value = "0.01") BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            @Min(1) int maxUses,
            Instant startsAt,
            Instant expiresAt,
            @Min(0) int minAccountAgeDays,
            @Min(1) int perUserLimit,
            VoucherAudienceType audienceType,
            boolean firstOrderOnly,
            @Min(0) int minCompletedOrders,
            BigDecimal minTotalSpent,
            Boolean active) {
        public VoucherCommand.Update toCommand() {
            return new VoucherCommand.Update(discountType, discountValue, maxDiscountAmount,
                    minOrderAmount, maxUses, startsAt, expiresAt, minAccountAgeDays,
                    perUserLimit, audienceType, firstOrderOnly, minCompletedOrders, minTotalSpent, active);
        }
    }

    public record ValidateVoucherRequest(
            @NotBlank String code,
            @NotNull @DecimalMin("0.00") BigDecimal orderSubtotal) {
        public VoucherCommand.Validate toCommand(UUID userId) {
            return new VoucherCommand.Validate(code, userId, orderSubtotal);
        }
    }

    public record InternalValidateVoucherRequest(
            @NotBlank String code,
            @NotNull UUID userId,
            @NotNull @DecimalMin("0.00") BigDecimal orderSubtotal) {
        public VoucherCommand.Validate toCommand() {
            return new VoucherCommand.Validate(code, userId, orderSubtotal);
        }
    }

    public record RedeemVoucherRequest(
            @NotBlank String code,
            @NotNull UUID userId,
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.00") BigDecimal orderSubtotal) {
        public VoucherCommand.Redeem toCommand() {
            return new VoucherCommand.Redeem(code, userId, orderId, orderSubtotal);
        }
    }

    public record AddAllowedUserRequest(@NotNull UUID userId) {}

    public record VoucherResponse(
            UUID id,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            int minAccountAgeDays,
            int perUserLimit,
            VoucherAudienceType audienceType,
            boolean firstOrderOnly,
            int minCompletedOrders,
            BigDecimal minTotalSpent,
            int maxUses,
            int usedCount,
            boolean active,
            Instant startsAt,
            Instant expiresAt) {
        public static VoucherResponse from(Voucher voucher) {
            return new VoucherResponse(
                    voucher.getId(),
                    voucher.getCode(),
                    voucher.getDiscountType(),
                    voucher.getDiscountValue(),
                    voucher.getMaxDiscountAmount(),
                    voucher.getMinOrderAmount(),
                    voucher.getMinAccountAgeDays(),
                    voucher.getPerUserLimit(),
                    voucher.getAudienceType(),
                    voucher.isFirstOrderOnly(),
                    voucher.getMinCompletedOrders(),
                    voucher.getMinTotalSpent(),
                    voucher.getMaxUses(),
                    voucher.getUsedCount(),
                    voucher.isActive(),
                    voucher.getStartsAt(),
                    voucher.getExpiresAt());
        }
    }

    public record AllowedUserResponse(UUID id, UUID voucherId, UUID userId) {
        public static AllowedUserResponse from(VoucherAllowedUser allowedUser) {
            return new AllowedUserResponse(
                    allowedUser.getId(),
                    allowedUser.getVoucherId(),
                    allowedUser.getUserId());
        }
    }

    public record VoucherValidationResponse(
            String code, BigDecimal discountAmount, String discountType, boolean valid, String message) {
        public static VoucherValidationResponse from(ValidateVoucherUseCase.Result result) {
            return new VoucherValidationResponse(
                    result.code(), result.discountAmount(), result.discountType(), result.valid(), result.message());
        }
    }

    public record VoucherRedeemResponse(UUID usageId, String code, BigDecimal discountAmount, boolean idempotent) {
        public static VoucherRedeemResponse from(RedeemVoucherUseCase.Result result) {
            return new VoucherRedeemResponse(
                    result.usageId(), result.code(), result.discountAmount(), result.idempotent());
        }
    }
}
