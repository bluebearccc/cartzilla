package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.VoucherAllowedUser;

import java.util.List;
import java.util.UUID;

public interface VoucherAllowedUserRepository {
    VoucherAllowedUser save(VoucherAllowedUser allowedUser);
    boolean existsByVoucherIdAndUserId(UUID voucherId, UUID userId);
    List<VoucherAllowedUser> findByVoucherId(UUID voucherId);
    void deleteByVoucherIdAndUserId(UUID voucherId, UUID userId);
}
