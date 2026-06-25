package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.domain.exception.UnprocessableEntityException;
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

import com.cartzilla.user.infrastructure.feign.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

@Service
public class ValidateVoucherUseCase {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherUsageRepository usageRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;
    private final OrderFeignClient orderFeignClient;

    public ValidateVoucherUseCase(
            VoucherRepository voucherRepository,
            UserRepository userRepository,
            VoucherUsageRepository usageRepository,
            VoucherAllowedUserRepository allowedUserRepository) {
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.usageRepository = usageRepository;
        this.allowedUserRepository = allowedUserRepository;
        this.orderFeignClient = null;
    }

    @Autowired
    public ValidateVoucherUseCase(
            VoucherRepository voucherRepository,
            UserRepository userRepository,
            VoucherUsageRepository usageRepository,
            VoucherAllowedUserRepository allowedUserRepository,
            OrderFeignClient orderFeignClient) {
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.usageRepository = usageRepository;
        this.allowedUserRepository = allowedUserRepository;
        this.orderFeignClient = orderFeignClient;
    }

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
        validateEligibility(voucher, user, subtotal, null);
    }

    void validateEligibility(Voucher voucher, User user, BigDecimal subtotal, UUID excludeOrderId) {
        subtotal = requireSubtotal(subtotal);
        Instant now = Instant.now();
        if (!voucher.isRedeemable(now)) {
            throw new UnprocessableEntityException("Voucher is not redeemable (VA-03)");
        }
        if (subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new UnprocessableEntityException("Order subtotal " + subtotal
                    + " < minOrderAmount " + voucher.getMinOrderAmount() + " (V-04)");
        }
        validateSupportedEligibilityRules(voucher, user, excludeOrderId);
        validateAccountAge(voucher, user, now);
        validatePerUserLimit(voucher, user);
        validateAudience(voucher, user, excludeOrderId);
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
            throw new UnprocessableEntityException("Cannot validate voucher account age");
        }
        long ageDays = ChronoUnit.DAYS.between(
                user.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                now.atZone(ZoneOffset.UTC).toLocalDate());
        if (ageDays < voucher.getMinAccountAgeDays()) {
            throw new UnprocessableEntityException("Voucher requires account age of at least "
                    + voucher.getMinAccountAgeDays() + " days. Current: " + ageDays + " days");
        }
    }

    private void validatePerUserLimit(Voucher voucher, User user) {
        long userUsageCount = usageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId());
        if (userUsageCount >= voucher.getPerUserLimit()) {
            throw new UnprocessableEntityException("User has reached perUserLimit="
                    + voucher.getPerUserLimit() + " for this voucher (V-11)");
        }
    }

    private void validateSupportedEligibilityRules(Voucher voucher, User user, UUID excludeOrderId) {
        if (voucher.isFirstOrderOnly()
                || voucher.getMinCompletedOrders() > 0
                || voucher.getMinTotalSpent().compareTo(BigDecimal.ZERO) > 0) {
            
            if (orderFeignClient == null) {
                throw new UnprocessableEntityException(
                        "Voucher order-history eligibility requires order-service user stats integration");
            }

            OrderFeignClient.UserOrderStatsDto stats;
            try {
                var response = orderFeignClient.getUserOrderStats(user.getId(), excludeOrderId);
                if (response == null || !response.success() || response.data() == null) {
                    throw new UnprocessableEntityException("Cannot fetch user order stats from order-service");
                }
                stats = response.data();
            } catch (Exception ex) {
                throw new UnprocessableEntityException("order-service communication failure: " + ex.getMessage());
            }

            if (voucher.isFirstOrderOnly() && stats.nonCancelledOrdersCount() > 0) {
                throw new UnprocessableEntityException("Voucher is only valid for the first order");
            }

            if (voucher.getMinCompletedOrders() > 0 && stats.completedOrdersCount() < voucher.getMinCompletedOrders()) {
                throw new UnprocessableEntityException("Voucher requires at least "
                        + voucher.getMinCompletedOrders() + " completed orders. Current: " + stats.completedOrdersCount());
            }

            if (voucher.getMinTotalSpent().compareTo(BigDecimal.ZERO) > 0 
                    && stats.totalSpent().compareTo(voucher.getMinTotalSpent()) < 0) {
                throw new UnprocessableEntityException("Voucher requires total spent of at least "
                        + voucher.getMinTotalSpent() + ". Current: " + stats.totalSpent());
            }
        }
    }

    private void validateAudience(Voucher voucher, User user, UUID excludeOrderId) {
        switch (voucher.getAudienceType()) {
            case ALL_USERS -> { /* hợp lệ với mọi user */ }
            case SPECIFIC_USERS -> {
                if (!allowedUserRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
                    throw new UnprocessableEntityException("Voucher audience mismatch: SPECIFIC_USERS");
                }
            }
            case NEW_CUSTOMER -> {
                if (orderFeignClient == null) {
                    throw new UnprocessableEntityException("Voucher audience NEW_CUSTOMER requires order-history integration");
                }
                try {
                    var response = orderFeignClient.getUserOrderStats(user.getId(), excludeOrderId);
                    if (response == null || !response.success() || response.data() == null || response.data().nonCancelledOrdersCount() > 0) {
                        throw new UnprocessableEntityException("Voucher is only valid for NEW_CUSTOMER (no past orders)");
                    }
                } catch (Exception e) {
                    throw new UnprocessableEntityException("order-service communication failure: " + e.getMessage());
                }
            }
            case LOYAL_CUSTOMER -> {
                if (orderFeignClient == null) {
                    throw new UnprocessableEntityException("Voucher audience LOYAL_CUSTOMER requires order-history integration");
                }
                try {
                    var response = orderFeignClient.getUserOrderStats(user.getId(), excludeOrderId);
                    if (response == null || !response.success() || response.data() == null || response.data().completedOrdersCount() < 3) {
                        throw new UnprocessableEntityException("Voucher is only valid for LOYAL_CUSTOMER (at least 3 completed orders)");
                    }
                } catch (Exception e) {
                    throw new UnprocessableEntityException("order-service communication failure: " + e.getMessage());
                }
            }
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
