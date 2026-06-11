package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F11 — UC-05: admin soft-delete product (BR-G03). Order cũ giữ snapshot (PA-06). */
@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;

    @Transactional
    public void execute(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.softDeleteCascade(); // BR-G03/PA-06: cascade xuống variant + image
        productRepository.save(product);
    }
}
