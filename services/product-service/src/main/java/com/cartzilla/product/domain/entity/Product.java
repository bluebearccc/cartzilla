package com.cartzilla.product.domain.entity;

import com.cartzilla.product.domain.vo.ProductVariant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
@Getter
@Setter
public class Product {

    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String slug;
    private String description;
    private String categoryId;
    private String categoryName;
    private String brand;
    private BigDecimal basePrice;
    private List<String> images = new ArrayList<>();
    private List<ProductVariant> variants = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private boolean active = true;
    private boolean featured = false;
    private boolean deleted = false;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
