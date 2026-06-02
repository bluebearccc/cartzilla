package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductMongoRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoryIdAndActiveTrueAndDeletedFalse(String categoryId);
    List<Product> findByActiveTrueAndDeletedFalse();
}
