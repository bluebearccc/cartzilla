package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.VendorCommand;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.product.domain.vo.VendorType;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F16 — UC-05: admin tạo vendor (VE-01..VE-02, VN-01..VN-02). */
@Service
@RequiredArgsConstructor
public class CreateVendorUseCase {

    private final VendorRepository vendorRepository;

    @Transactional
    public Vendor execute(VendorCommand.Create cmd) {
        Slug slug = (cmd.slug() == null || cmd.slug().isBlank())
                ? Slug.fromName(cmd.name(), 160)
                : Slug.of(cmd.slug(), 160);
        if (vendorRepository.existsBySlug(slug.getValue()))
            throw new BusinessException("Vendor slug already exists (VE-01): " + slug);

        Vendor vendor = Vendor.create(cmd.name(), slug.getValue(), parseType(cmd.vendorType()),
                cmd.contactEmail(), cmd.phone(), cmd.website(), cmd.logoUrl());
        return vendorRepository.save(vendor);
    }

    static VendorType parseType(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("Vendor type must not be blank (VE-02)");
        try {
            return VendorType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid vendor type (VE-02): " + raw
                    + " — expected SUPPLIER|BRAND|MANUFACTURER");
        }
    }
}
