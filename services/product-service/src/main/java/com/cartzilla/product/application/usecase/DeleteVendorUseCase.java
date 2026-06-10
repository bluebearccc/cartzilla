package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * F16 — UC-05: admin soft-delete vendor.
 * BR-P08: product cũ vẫn giữ vendorId reference/snapshot.
 */
@Service
@RequiredArgsConstructor
public class DeleteVendorUseCase {

    private final VendorRepository vendorRepository;

    @Transactional
    public void execute(UUID id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
        vendor.deactivate();
        vendor.softDelete();
        vendorRepository.save(vendor);
    }
}
