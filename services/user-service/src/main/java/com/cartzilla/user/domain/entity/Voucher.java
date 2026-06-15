package com.cartzilla.user.domain.entity;

import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "min_account_age_days", nullable = false)
    private int minAccountAgeDays = 0;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private VoucherAudienceType audienceType = VoucherAudienceType.ALL_USERS;

    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly = false;

    @Column(name = "min_completed_orders", nullable = false)
    private int minCompletedOrders = 0;

    @Column(name = "min_total_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal minTotalSpent = BigDecimal.ZERO;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static Voucher create(String code, DiscountType discountType, BigDecimal discountValue,
                                 BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                 int maxUses, Instant startsAt, Instant expiresAt,
                                 int minAccountAgeDays, int perUserLimit,
                                 VoucherAudienceType audienceType, boolean firstOrderOnly,
                                 int minCompletedOrders, BigDecimal minTotalSpent) {
        validateCode(code);
        validateRules(discountType, discountValue, maxDiscountAmount, minOrderAmount, maxUses,
                startsAt, expiresAt, minAccountAgeDays, perUserLimit);

        Voucher voucher = new Voucher();
        voucher.code = code.trim().toUpperCase();
        voucher.discountType = discountType;
        voucher.discountValue = discountValue;
        voucher.maxDiscountAmount = maxDiscountAmount;
        voucher.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        voucher.maxUses = maxUses;
        voucher.usedCount = 0;
        voucher.active = true;
        voucher.startsAt = startsAt;
        voucher.expiresAt = expiresAt;
        voucher.minAccountAgeDays = minAccountAgeDays;
        voucher.perUserLimit = perUserLimit;
        voucher.audienceType = audienceType != null ? audienceType : VoucherAudienceType.ALL_USERS;
        voucher.firstOrderOnly = firstOrderOnly;
        voucher.minCompletedOrders = minCompletedOrders;
        voucher.minTotalSpent = minTotalSpent != null ? minTotalSpent : BigDecimal.ZERO;
        return voucher;
    }

    public boolean isRedeemable(Instant now) {
        if (!active) return false;
        if (usedCount >= maxUses) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        return expiresAt == null || now.isBefore(expiresAt);
    }

    public void incrementUsedCount() {
        if (usedCount >= maxUses) {
            throw new BusinessException("Voucher has reached max uses (VA-02)");
        }
        this.usedCount++;
    }

    public void updateRules(DiscountType discountType, BigDecimal discountValue,
                            BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                            int maxUses, Instant startsAt, Instant expiresAt,
                            int minAccountAgeDays, int perUserLimit,
                            VoucherAudienceType audienceType, boolean firstOrderOnly,
                            int minCompletedOrders, BigDecimal minTotalSpent,
                            Boolean active) {
        validateRules(discountType, discountValue, maxDiscountAmount, minOrderAmount, maxUses,
                startsAt, expiresAt, minAccountAgeDays, perUserLimit);
        if (maxUses < this.usedCount) {
            throw new BusinessException("maxUses cannot be lower than usedCount");
        }

        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        this.maxUses = maxUses;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.minAccountAgeDays = minAccountAgeDays;
        this.perUserLimit = perUserLimit;
        this.audienceType = audienceType != null ? audienceType : VoucherAudienceType.ALL_USERS;
        this.firstOrderOnly = firstOrderOnly;
        this.minCompletedOrders = minCompletedOrders;
        this.minTotalSpent = minTotalSpent != null ? minTotalSpent : BigDecimal.ZERO;
        if (active != null) {
            this.active = active;
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Voucher code must not be blank");
        }
    }

    private static void validateRules(DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                      int maxUses, Instant startsAt, Instant expiresAt,
                                      int minAccountAgeDays, int perUserLimit) {
        if (discountType == null) {
            throw new BusinessException("discountType is required");
        }
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0
                    || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("PERCENTAGE discountValue must be 0 < x <= 100 (V-02)");
            }
            if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("maxDiscountAmount required and > 0 for PERCENTAGE (V-02, BR-V08)");
            }
        } else if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("FIXED_AMOUNT discountValue must be > 0 (V-03)");
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("minOrderAmount must be >= 0 (V-04)");
        }
        if (maxUses < 1) {
            throw new BusinessException("maxUses must be >= 1 (V-05)");
        }
        if (expiresAt != null && startsAt != null && !expiresAt.isAfter(startsAt)) {
            throw new BusinessException("expiresAt must be after startsAt (V-06)");
        }
        if (minAccountAgeDays < 0) {
            throw new BusinessException("minAccountAgeDays must be >= 0 (V-08)");
        }
        if (perUserLimit < 1) {
            throw new BusinessException("perUserLimit must be >= 1");
        }
    }
}
