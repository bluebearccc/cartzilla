package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoucherJpaRepository extends JpaRepository<Voucher, UUID> {

    @Query("SELECT v FROM Voucher v WHERE UPPER(v.code) = UPPER(:code)")
    Optional<Voucher> findByCodeIgnoreCase(@Param("code") String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id AND v.usedCount < v.maxUses")
    int incrementUsedCountConditional(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 WHERE v.id = :id AND v.usedCount > 0")
    int decrementUsedCountIfPositive(@Param("id") UUID id);
}
