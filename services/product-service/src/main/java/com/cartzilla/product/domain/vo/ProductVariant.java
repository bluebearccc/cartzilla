package com.cartzilla.product.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Value object — biến thể size/màu, embedded trong Product document. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    private String sku;
    private String size;
    private String color;
    private String colorHex;
    private BigDecimal price;
    private int stock;
}
