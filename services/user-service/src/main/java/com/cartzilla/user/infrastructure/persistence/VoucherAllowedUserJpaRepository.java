package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.VoucherAllowedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoucherAllowedUserJpaRepository extends JpaRepository<VoucherAllowedUser, UUID> {
    boolean existsByVoucherIdAndUserId(UUID voucherId, UUID userId);
    List<VoucherAllowedUser> findByVoucherId(UUID voucherId);
    void deleteByVoucherIdAndUserId(UUID voucherId, UUID userId);
}
