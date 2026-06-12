package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.Voucher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository {
    Voucher save(Voucher voucher);
    Optional<Voucher> findById(UUID id);
    Optional<Voucher> findByCode(String code);
    List<Voucher> findAll();
    boolean existsByCode(String code);
    int incrementUsedCountIfAvailable(UUID id);
}
