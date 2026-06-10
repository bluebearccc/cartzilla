package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.vo.ColorHex;
import com.cartzilla.product.domain.vo.Sku;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * F11 — UC-05: admin thêm/sửa/xóa variant qua aggregate root.
 * Rules: PV-01..PV-05.
 */
@Service
@RequiredArgsConstructor
public class ManageVariantUseCase {

    private final ProductRepository productRepository;

    @Transactional
    public Product add(UUID productId, ProductCommand.VariantData cmd) {
        Product product = load(productId);
        Sku sku = Sku.of(cmd.sku());
        if (productRepository.existsBySku(sku.getValue()))
            throw new BusinessException("SKU already exists (PV-01): " + sku);
        product.addVariant(ProductVariant.create(sku.getValue(), cmd.size(), cmd.color(),
                normalizeHex(cmd.colorHex()), cmd.price(), cmd.stock()));
        return productRepository.save(product);
    }

    @Transactional
    public Product update(UUID productId, UUID variantId, ProductCommand.UpdateVariant cmd) {
        Product product = load(productId);
        product.updateVariant(variantId, cmd.size(), cmd.color(),
                normalizeHex(cmd.colorHex()), cmd.price(), cmd.stock());
        return productRepository.save(product);
    }

    @Transactional
    public Product remove(UUID productId, UUID variantId) {
        Product product = load(productId);
        product.removeVariant(variantId); // PV-04 guard
        return productRepository.save(product);
    }

    private Product load(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private static String normalizeHex(String colorHex) {
        return (colorHex == null || colorHex.isBlank()) ? null : ColorHex.of(colorHex).getValue();
    }
}
