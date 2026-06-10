package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.infrastructure.persistence.VendorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VendorRepositoryAdapter implements VendorRepository {

    private final VendorJpaRepository jpa;

    @Override public Vendor save(Vendor v) { return jpa.save(v); }

    @Override public Optional<Vendor> findById(UUID id) { return jpa.findById(id); }

    @Override public Optional<Vendor> findBySlug(String slug) { return jpa.findBySlug(slug); }

    @Override public List<Vendor> findAllActive() { return jpa.findByActiveTrueOrderByNameAsc(); }

    @Override public List<Vendor> findAll() { return jpa.findAllByOrderByNameAsc(); }

    @Override public boolean existsBySlug(String slug) { return jpa.existsBySlug(slug); }
}
