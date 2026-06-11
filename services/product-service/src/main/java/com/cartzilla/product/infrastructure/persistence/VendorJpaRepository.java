package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorJpaRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findBySlug(String slug);

    /** VE-01: slug unique */
    boolean existsBySlug(String slug);

    List<Vendor> findByActiveTrueOrderByNameAsc();

    List<Vendor> findAllByOrderByNameAsc();
}
