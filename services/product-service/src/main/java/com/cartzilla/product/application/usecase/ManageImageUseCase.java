package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * F11 — UC-05: admin thêm/xóa/đặt primary image qua aggregate root.
 * Rules: PI-01..PI-03, PA-05.
 */
@Service
@RequiredArgsConstructor
public class ManageImageUseCase {

    private final ProductRepository productRepository;

    @Transactional
    public Product add(UUID productId, ProductCommand.ImageData cmd) {
        Product product = load(productId);
        product.addImage(ProductImage.create(cmd.imageUrl(), cmd.altText(), cmd.isPrimary(), cmd.sortOrder()));
        product.ensurePrimaryImage(); // PA-05
        return productRepository.save(product);
    }

    @Transactional
    public Product setPrimary(UUID productId, UUID imageId) {
        Product product = load(productId);
        product.setPrimaryImage(imageId); // PI-03
        return productRepository.save(product);
    }

    @Transactional
    public Product remove(UUID productId, UUID imageId) {
        Product product = load(productId);
        product.removeImage(imageId);
        return productRepository.save(product);
    }

    private Product load(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }
}
