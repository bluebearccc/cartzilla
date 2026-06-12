package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.VoucherUsage;

import java.util.Optional;
import java.util.UUID;

public interface VoucherUsageRepository {
    VoucherUsage save(VoucherUsage usage);
    Optional<VoucherUsage> findByVoucherIdAndOrderId(UUID voucherId, UUID orderId);
    long countByVoucherIdAndUserId(UUID voucherId, UUID userId);
}
