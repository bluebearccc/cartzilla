package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListProductsUseCase {

    private final ProductRepository productRepository;

    public List<Product> execute(UUID categoryId) {
        return (categoryId == null)
                ? productRepository.findAllActive()
                : productRepository.findByCategory(categoryId);
    }
}
