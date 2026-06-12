package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.infrastructure.persistence.VoucherJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoucherRepositoryAdapter implements VoucherRepository {

    private final VoucherJpaRepository jpa;

    @Override
    public Voucher save(Voucher voucher) {
        return jpa.save(voucher);
    }

    @Override
    public Optional<Voucher> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Voucher> findByCode(String code) {
        return jpa.findByCodeIgnoreCase(code);
    }

    @Override
    public List<Voucher> findAll() {
        return jpa.findAll();
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.findByCodeIgnoreCase(code).isPresent();
    }

    @Override
    public int incrementUsedCountIfAvailable(UUID id) {
        return jpa.incrementUsedCountConditional(id);
    }
}
