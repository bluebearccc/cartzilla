package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.Vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PORT — domain định nghĩa, infrastructure implement. */
public interface VendorRepository {
    Vendor save(Vendor vendor);
    Optional<Vendor> findById(UUID id);
    Optional<Vendor> findBySlug(String slug);
    List<Vendor> findAllActive();
    List<Vendor> findAll();
    boolean existsBySlug(String slug);
}
