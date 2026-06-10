package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/** F11 — UC-05: admin cập nhật product (thông tin chung + active/featured). */
@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;

    @Transactional
    public Product execute(UUID id, ProductCommand.Update cmd) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        // P-01: chỉ validate active khi đổi sang category khác
        if (!Objects.equals(cmd.categoryId(), product.getCategoryId())) {
            Category category = categoryRepository.findById(cmd.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cmd.categoryId()));
            if (!category.isActive())
                throw new BusinessException("Category must be active (P-01)");
        }

        // BR-P08: vendor inactive không được gán mới; vendor cũ giữ nguyên được
        if (cmd.vendorId() != null && !Objects.equals(cmd.vendorId(), product.getVendorId())) {
            Vendor vendor = vendorRepository.findById(cmd.vendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + cmd.vendorId()));
            if (!vendor.isActive())
                throw new BusinessException("Cannot assign inactive vendor (BR-P08)");
        }

        product.update(cmd.categoryId(), cmd.vendorId(), cmd.name(),
                cmd.description(), cmd.basePrice(), cmd.tags());

        // PA-04: đổi slug phải unique
        if (cmd.slug() != null && !cmd.slug().isBlank()) {
            Slug slug = Slug.of(cmd.slug(), 220);
            if (!slug.getValue().equals(product.getSlug())) {
                if (productRepository.existsBySlug(slug.getValue()))
                    throw new BusinessException("Product slug already exists (PA-04): " + slug);
                product.changeSlug(slug.getValue());
            }
        }

        if (cmd.active() != null) {
            if (cmd.active()) product.activate(); else product.deactivate();
        }
        if (cmd.featured() != null) {
            product.setFeatured(cmd.featured()); // P-05: featured chỉ khi active
        }

        return productRepository.save(product);
    }
}
