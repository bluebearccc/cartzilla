package com.cartzilla.user.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "voucher_allowed_users",
        uniqueConstraints = @UniqueConstraint(name = "uq_voucher_allowed_user",
                columnNames = {"voucher_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoucherAllowedUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public static VoucherAllowedUser add(UUID voucherId, UUID userId) {
        if (voucherId == null) throw new BusinessException("voucherId must not be null");
        if (userId == null) throw new BusinessException("userId must not be null");
        VoucherAllowedUser vau = new VoucherAllowedUser();
        vau.voucherId = voucherId;
        vau.userId = userId;
        return vau;
    }
}
