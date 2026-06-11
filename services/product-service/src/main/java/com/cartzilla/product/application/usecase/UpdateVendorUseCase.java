package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.VendorCommand;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F16 — UC-05: admin cập nhật/deactivate vendor (VE-03). */
@Service
@RequiredArgsConstructor
public class UpdateVendorUseCase {

    private final VendorRepository vendorRepository;

    @Transactional
    public Vendor execute(UUID id, VendorCommand.Update cmd) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));

        vendor.update(cmd.name(), CreateVendorUseCase.parseType(cmd.vendorType()),
                cmd.contactEmail(), cmd.phone(), cmd.website(), cmd.logoUrl());

        if (cmd.slug() != null && !cmd.slug().isBlank()) {
            Slug slug = Slug.of(cmd.slug(), 160);
            if (!slug.getValue().equals(vendor.getSlug())) {
                if (vendorRepository.existsBySlug(slug.getValue()))
                    throw new BusinessException("Vendor slug already exists (VE-01): " + slug);
                vendor.changeSlug(slug.getValue());
            }
        }

        if (cmd.active() != null) {
            // VE-03: deactivate không ảnh hưởng product cũ
            if (cmd.active()) vendor.activate(); else vendor.deactivate();
        }

        return vendorRepository.save(vendor);
    }
}
