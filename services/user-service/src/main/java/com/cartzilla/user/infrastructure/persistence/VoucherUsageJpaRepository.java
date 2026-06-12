package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoucherUsageJpaRepository extends JpaRepository<VoucherUsage, UUID> {

    /** VA-04/VU-04: idempotent check */
    boolean existsByVoucherIdAndOrderId(UUID voucherId, UUID orderId);

    Optional<VoucherUsage> findByVoucherIdAndOrderId(UUID voucherId, UUID orderId);

    /** V-11/BR-V09: đếm lượt redeem của user */
    long countByVoucherIdAndUserId(UUID voucherId, UUID userId);
}
