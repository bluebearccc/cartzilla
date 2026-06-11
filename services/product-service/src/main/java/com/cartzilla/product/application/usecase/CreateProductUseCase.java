package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.entity.Vendor;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.VendorRepository;
import com.cartzilla.product.domain.vo.ColorHex;
import com.cartzilla.product.domain.vo.Sku;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** F11 — UC-05: admin tạo product (kèm variants/images ban đầu nếu có). */
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;

    @Transactional
    public Product execute(ProductCommand.Create cmd) {
        Category category = categoryRepository.findById(cmd.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + cmd.categoryId()));
        if (!category.isActive())
            throw new BusinessException("Category must be active to create product (P-01)");

        if (cmd.vendorId() != null) {
            Vendor vendor = vendorRepository.findById(cmd.vendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + cmd.vendorId()));
            if (!vendor.isActive())
                throw new BusinessException("Cannot assign inactive vendor to new product (BR-P08)");
        }

        // PA-04/BR-P02: slug auto-generate từ name nếu không nhập
        Slug slug = (cmd.slug() == null || cmd.slug().isBlank())
                ? Slug.fromName(cmd.name(), 220)
                : Slug.of(cmd.slug(), 220);
        if (productRepository.existsBySlug(slug.getValue()))
            throw new BusinessException("Product slug already exists (PA-04): " + slug);

        Product product = Product.create(cmd.categoryId(), cmd.name(), slug.getValue(),
                cmd.description(), cmd.basePrice(), cmd.vendorId(), cmd.tags());

        if (cmd.variants() != null) {
            Set<String> requestSkus = new HashSet<>();
            for (ProductCommand.VariantData v : cmd.variants()) {
                Sku sku = Sku.of(v.sku());
                if (!requestSkus.add(sku.getValue()))
                    throw new BusinessException("Duplicate SKU in request (PV-01): " + sku);
                if (productRepository.existsBySku(sku.getValue()))
                    throw new BusinessException("SKU already exists (PV-01): " + sku);
                product.addVariant(ProductVariant.create(sku.getValue(), v.size(), v.color(),
                        normalizeHex(v.colorHex()), v.price(), v.stock()));
            }
        }

        if (cmd.images() != null) {
            for (ProductCommand.ImageData i : cmd.images()) {
                product.addImage(ProductImage.create(i.imageUrl(), i.altText(), i.isPrimary(), i.sortOrder()));
            }
            product.ensurePrimaryImage(); // PA-05
        }

        return productRepository.save(product);
    }

    private static String normalizeHex(String colorHex) {
        return (colorHex == null || colorHex.isBlank()) ? null : ColorHex.of(colorHex).getValue();
    }
}
