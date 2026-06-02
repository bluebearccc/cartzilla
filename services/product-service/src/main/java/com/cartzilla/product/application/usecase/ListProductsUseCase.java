package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListProductsUseCase {

    private final ProductRepository productRepository;

    public List<Product> execute(String categoryId) {
        return (categoryId == null || categoryId.isBlank())
                ? productRepository.findAllActive()
                : productRepository.findByCategory(categoryId);
    }
}
