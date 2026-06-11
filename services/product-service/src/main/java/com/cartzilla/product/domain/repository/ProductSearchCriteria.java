package com.cartzilla.product.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Query object cho port ProductRepository.search (F03 — UC-01).
 * activeOnly = TRUE → public catalog: chỉ product active, category active,
 * sellable (≥1 variant + ≥1 image — BR-P01); null → admin xem tất cả.
 */
public record ProductSearchCriteria(
        UUID categoryId,
        String categorySlug,
        String keyword,
        String size,
        String color,
        UUID vendorId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStockOnly,
        Boolean featured,
        Boolean activeOnly) {

    public static ProductSearchCriteria publicCatalog(UUID categoryId, String categorySlug,
                                                      String keyword, String size, String color,
                                                      UUID vendorId, BigDecimal minPrice, BigDecimal maxPrice,
                                                      Boolean inStockOnly, Boolean featured) {
        return new ProductSearchCriteria(categoryId, categorySlug, keyword, size, color,
                vendorId, minPrice, maxPrice, inStockOnly, featured, true);
    }

    public static ProductSearchCriteria admin(UUID categoryId, String keyword) {
        return new ProductSearchCriteria(categoryId, null, keyword, null, null,
                null, null, null, null, null, null);
    }
}
