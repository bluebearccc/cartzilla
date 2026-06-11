package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F03 — UC-01: browse/search/filter/sort/phân trang catalog. */
@Service
@RequiredArgsConstructor
public class ListProductsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<Product> execute(ProductSearchCriteria criteria, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return productRepository.search(criteria, PageRequest.of(safePage, safeSize, mapSort(sort)));
    }

    /** Whitelist sort key — tránh client sort theo field tùy ý */
    private static Sort mapSort(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "price_asc"  -> Sort.by(Sort.Order.asc("basePrice"), Sort.Order.desc("createdAt"));
            case "price_desc" -> Sort.by(Sort.Order.desc("basePrice"), Sort.Order.desc("createdAt"));
            case "featured"   -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("createdAt"));
            case "name"       -> Sort.by(Sort.Order.asc("name"));
            default           -> Sort.by(Sort.Order.desc("createdAt")); // newest
        };
    }
}
