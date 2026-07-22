package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.VoucherUsage;
import com.cartzilla.user.domain.repository.VoucherUsageRepository;
import com.cartzilla.user.infrastructure.persistence.VoucherUsageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoucherUsageRepositoryAdapter implements VoucherUsageRepository {

    private final VoucherUsageJpaRepository jpa;

    @Override
    public VoucherUsage save(VoucherUsage usage) {
        return jpa.save(usage);
    }

    @Override
    public Optional<VoucherUsage> findByVoucherIdAndOrderId(UUID voucherId, UUID orderId) {
        return jpa.findByVoucherIdAndOrderId(voucherId, orderId);
    }

    @Override
    public long countByVoucherIdAndUserId(UUID voucherId, UUID userId) {
        return jpa.countByVoucherIdAndUserIdAndReleasedFalse(voucherId, userId);
    }
}
