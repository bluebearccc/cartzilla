package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F04 — UC-01: chi tiết sản phẩm (ảnh, variant, SKU, giá, tồn kho). */
@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /** Public detail — ẩn product inactive/category inactive (UC-01 A2, PA-03). */
    @Transactional(readOnly = true)
    public Product execute(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return assertPubliclyVisible(product);
    }

    @Transactional(readOnly = true)
    public Product executeBySlug(String slug) {
        Product product = productRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return assertPubliclyVisible(product);
    }

    /** Admin detail — xem được cả product inactive. */
    @Transactional(readOnly = true)
    public Product executeForAdmin(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Product assertPubliclyVisible(Product product) {
        boolean categoryActive = categoryRepository.findById(product.getCategoryId())
                .map(Category::isActive)
                .orElse(false);
        if (!product.isActive() || !categoryActive) {
            throw new ResourceNotFoundException("Product not found: " + product.getId());
        }
        return product;
    }
}
