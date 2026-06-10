package com.cartzilla.user.domain.entity;

import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root — VoucherAggregate.
 * Rules: VA-01..VA-09, V-01..V-14.
 * code unique via functional index upper(code) — VA-01.
 */
@Entity
@Table(name = "vouchers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Voucher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** VA-01: normalize uppercase ở application layer */
    @Column(nullable = false, length = 50)
    private String code;

    /** V-01 */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** V-02/V-03 */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** V-02/BR-V08: bắt buộc với PERCENTAGE */
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** V-04: ≥ 0 */
    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** VA-06, V-08: ≥ 0, default 0 */
    @Column(name = "min_account_age_days", nullable = false)
    private int minAccountAgeDays = 0;

    /** V-11, BR-V09: ≥ 1 */
    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;

    /** V-14, BR-V14 */
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private VoucherAudienceType audienceType = VoucherAudienceType.ALL_USERS;

    /** V-12 */
    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly = false;

    /** V-13 */
    @Column(name = "min_completed_orders", nullable = false)
    private int minCompletedOrders = 0;

    /** V-13 */
    @Column(name = "min_total_spent", nullable = false, precision = 12, scale = 2)
    private BigDecimal minTotalSpent = BigDecimal.ZERO;

    /** V-05: ≥ 1 */
    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    /** V-05: starts 0, incremented atomically */
    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** V-10 */
    @Column(name = "starts_at")
    private Instant startsAt;

    /** V-06 */
    @Column(name = "expires_at")
    private Instant expiresAt;

    public static Voucher create(String code, DiscountType discountType, BigDecimal discountValue,
                                  BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                  int maxUses, Instant startsAt, Instant expiresAt,
                                  int minAccountAgeDays, int perUserLimit,
                                  VoucherAudienceType audienceType, boolean firstOrderOnly,
                                  int minCompletedOrders, BigDecimal minTotalSpent) {
        if (code == null || code.isBlank()) throw new BusinessException("Voucher code must not be blank");
        // V-01
        if (discountType == DiscountType.PERCENTAGE) {
            // V-02
            if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0
                    || discountValue.compareTo(BigDecimal.valueOf(100)) > 0)
                throw new BusinessException("PERCENTAGE discountValue must be 0 < x <= 100 (V-02)");
            if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0)
                throw new BusinessException("maxDiscountAmount required and > 0 for PERCENTAGE (V-02, BR-V08)");
        } else {
            // V-03
            if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0)
                throw new BusinessException("FIXED_AMOUNT discountValue must be > 0 (V-03)");
        }
        // V-04
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("minOrderAmount must be >= 0 (V-04)");
        // V-05
        if (maxUses < 1) throw new BusinessException("maxUses must be >= 1 (V-05)");
        // V-06
        if (expiresAt != null && startsAt != null && !expiresAt.isAfter(startsAt))
            throw new BusinessException("expiresAt must be after startsAt (V-06)");
        // V-08
        if (minAccountAgeDays < 0) throw new BusinessException("minAccountAgeDays must be >= 0 (V-08)");
        // per-user limit
        if (perUserLimit < 1) throw new BusinessException("perUserLimit must be >= 1");

        Voucher v = new Voucher();
        v.code = code.trim().toUpperCase();
        v.discountType = discountType;
        v.discountValue = discountValue;
        v.maxDiscountAmount = maxDiscountAmount;
        v.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        v.maxUses = maxUses;
        v.usedCount = 0;
        v.active = true;
        v.startsAt = startsAt;
        v.expiresAt = expiresAt;
        v.minAccountAgeDays = minAccountAgeDays;
        v.perUserLimit = perUserLimit;
        v.audienceType = audienceType != null ? audienceType : VoucherAudienceType.ALL_USERS;
        v.firstOrderOnly = firstOrderOnly;
        v.minCompletedOrders = minCompletedOrders;
        v.minTotalSpent = minTotalSpent != null ? minTotalSpent : BigDecimal.ZERO;
        return v;
    }

    /** VA-02/VA-03: kiểm tra có thể redeem không */
    public boolean isRedeemable(Instant now) {
        if (!active) return false;
        if (usedCount >= maxUses) return false;
        if (startsAt != null && now.isBefore(startsAt)) return false;
        if (expiresAt != null && !now.isBefore(expiresAt)) return false;
        return true;
    }

    /** VA-02: tăng usedCount — phải gọi atomically qua conditional UPDATE */
    public void incrementUsedCount() {
        if (usedCount >= maxUses)
            throw new BusinessException("Voucher has reached max uses (VA-02)");
        this.usedCount++;
    }

    /** V-07: không sửa code sau khi có VoucherUsage */
    public void deactivate() { this.active = false; }
}
