package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.domain.repository.VoucherUsageRepository;
import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ValidateVoucherUseCase {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherUsageRepository usageRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;

    public record Result(String code, BigDecimal discountAmount,
                         String discountType, boolean valid, String message) {}

    public Result execute(VoucherCommand.Validate command) {
        Voucher voucher = voucherRepository.findByCode(requireCode(command.code()))
                .orElseThrow(() -> new BusinessException("Voucher not found: " + command.code()));
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException("User not found: " + command.userId()));
        user.requireActive();

        BigDecimal subtotal = requireSubtotal(command.orderSubtotal());
        validateEligibility(voucher, user, subtotal);
        BigDecimal discount = calculateDiscount(voucher, subtotal);

        return new Result(voucher.getCode(), discount, voucher.getDiscountType().name(), true, "Voucher valid");
    }

    void validateEligibility(Voucher voucher, User user, BigDecimal subtotal) {
        subtotal = requireSubtotal(subtotal);
        Instant now = Instant.now();
        if (!voucher.isRedeemable(now)) {
            throw new BusinessException("Voucher is not redeemable (VA-03)");
        }
        if (subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new BusinessException("Order subtotal " + subtotal
                    + " < minOrderAmount " + voucher.getMinOrderAmount() + " (V-04)");
        }
        validateAccountAge(voucher, user, now);
        validatePerUserLimit(voucher, user);
        validateAudience(voucher, user);
    }

    BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount = switch (voucher.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal raw = subtotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yield voucher.getMaxDiscountAmount() != null
                        ? raw.min(voucher.getMaxDiscountAmount())
                        : raw;
            }
            case FIXED_AMOUNT -> voucher.getDiscountValue().min(subtotal);
        };
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateAccountAge(Voucher voucher, User user, Instant now) {
        if (voucher.getMinAccountAgeDays() <= 0) {
            return;
        }
        if (user.getCreatedAt() == null) {
            throw new BusinessException("Cannot validate voucher account age");
        }
        long ageDays = ChronoUnit.DAYS.between(
                user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                now.atZone(ZoneOffset.UTC).toLocalDate());
        if (ageDays < voucher.getMinAccountAgeDays()) {
            throw new BusinessException("Voucher requires account age of at least "
                    + voucher.getMinAccountAgeDays() + " days. Current: " + ageDays + " days");
        }
    }

    private void validatePerUserLimit(Voucher voucher, User user) {
        long userUsageCount = usageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId());
        if (userUsageCount >= voucher.getPerUserLimit()) {
            throw new BusinessException("User has reached perUserLimit="
                    + voucher.getPerUserLimit() + " for this voucher (V-11)");
        }
    }

    private void validateAudience(Voucher voucher, User user) {
        VoucherAudienceType audienceType = voucher.getAudienceType();
        if (audienceType == VoucherAudienceType.SPECIFIC_USERS
                && !allowedUserRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
            throw new BusinessException("Voucher audience mismatch: SPECIFIC_USERS");
        }
        if (audienceType == VoucherAudienceType.NEW_CUSTOMER && !voucher.isFirstOrderOnly()) {
            throw new BusinessException("NEW_CUSTOMER voucher must be first-order-only");
        }
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Voucher code is required");
        }
        return code.trim();
    }

    private static BigDecimal requireSubtotal(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("orderSubtotal must be >= 0");
        }
        return subtotal;
    }
}
