package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoucherUsageJpaRepository extends JpaRepository<VoucherUsage, UUID> {

    boolean existsByVoucherIdAndOrderId(UUID voucherId, UUID orderId);

    Optional<VoucherUsage> findByVoucherIdAndOrderId(UUID voucherId, UUID orderId);

    long countByVoucherIdAndUserIdAndReleasedFalse(UUID voucherId, UUID userId);
}
