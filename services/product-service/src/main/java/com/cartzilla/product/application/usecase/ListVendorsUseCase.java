package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC-01/F16: danh sách vendor (public filter: active; admin: tất cả). */
@Service
@RequiredArgsConstructor
public class ListVendorsUseCase {

    private final VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<Vendor> execute(boolean includeInactive) {
        return includeInactive ? vendorRepository.findAll() : vendorRepository.findAllActive();
    }
}
