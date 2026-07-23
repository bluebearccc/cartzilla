package com.cartzilla.user.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "voucher_usages",
        uniqueConstraints = @UniqueConstraint(name = "uq_voucher_usage_order",
                columnNames = {"voucher_id", "order_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoucherUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private boolean released = false;

    public static VoucherUsage redeem(UUID voucherId, UUID userId, UUID orderId, BigDecimal discountAmount) {
        if (voucherId == null) throw new BusinessException("voucherId must not be null");
        if (userId == null) throw new BusinessException("userId must not be null (VU-02)");
        if (orderId == null) throw new BusinessException("orderId must not be null (VU-02)");
        VoucherUsage vu = new VoucherUsage();
        vu.voucherId = voucherId;
        vu.userId = userId;
        vu.orderId = orderId;
        vu.discountAmount = discountAmount;
        return vu;
    }

    public void release() {
        this.released = true;
    }
}
