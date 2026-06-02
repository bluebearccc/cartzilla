package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.infrastructure.persistence.ProductMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductMongoRepository mongo;

    @Override public Product save(Product p) { return mongo.save(p); }
    @Override public Optional<Product> findById(String id) { return mongo.findById(id); }
    @Override public List<Product> findByCategory(String categoryId) {
        return mongo.findByCategoryIdAndActiveTrueAndDeletedFalse(categoryId);
    }
    @Override public List<Product> findAllActive() {
        return mongo.findByActiveTrueAndDeletedFalse();
    }
}
