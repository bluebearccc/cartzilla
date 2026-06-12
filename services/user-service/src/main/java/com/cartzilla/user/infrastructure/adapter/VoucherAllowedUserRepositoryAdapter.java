package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.VoucherAllowedUser;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.infrastructure.persistence.VoucherAllowedUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoucherAllowedUserRepositoryAdapter implements VoucherAllowedUserRepository {

    private final VoucherAllowedUserJpaRepository jpa;

    @Override
    public VoucherAllowedUser save(VoucherAllowedUser allowedUser) {
        return jpa.save(allowedUser);
    }

    @Override
    public boolean existsByVoucherIdAndUserId(UUID voucherId, UUID userId) {
        return jpa.existsByVoucherIdAndUserId(voucherId, userId);
    }

    @Override
    public List<VoucherAllowedUser> findByVoucherId(UUID voucherId) {
        return jpa.findByVoucherId(voucherId);
    }

    @Override
    @Transactional
    public void deleteByVoucherIdAndUserId(UUID voucherId, UUID userId) {
        jpa.deleteByVoucherIdAndUserId(voucherId, userId);
    }
}
