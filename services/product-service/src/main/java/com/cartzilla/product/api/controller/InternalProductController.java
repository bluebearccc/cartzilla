package com.cartzilla.product.api.controller;

import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.infrastructure.persistence.ProductVariantJpaRepository;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Internal endpoints cho product-service.
 * Dùng bởi order-service qua Feign để:
 * - Lấy variant snapshot (CTA-05, PA-06).
 * - Reserve/release stock (PA-07, X-01, X-04).
 */
@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductVariantJpaRepository variantRepository;

    @GetMapping("/variants/{sku}")
    public ApiResponse<VariantSnapshotDto> getVariantBySku(@PathVariable String sku) {
        ProductVariant v = variantRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new BusinessException("Variant not found: " + sku));
        var product = v.getProduct();
        String primaryImage = product.getImages().stream()
                .filter(i -> i.isPrimary())
                .map(i -> i.getImageUrl())
                .findFirst()
                .orElse(null);
        return ApiResponse.ok(new VariantSnapshotDto(
                product.getId(), v.getId(), v.getSku(),
                product.getName(), primaryImage,
                v.getSize(), v.getColor(), v.getPrice(),
                v.getStock(), product.isActive() && !v.isDeleted()));
    }

    @PutMapping("/variants/reserve")
    @Transactional
    public ApiResponse<Void> reserveStock(@RequestBody List<StockRequest> requests) {
        for (StockRequest req : requests) {
            ProductVariant v = variantRepository.findBySkuIgnoreCase(req.sku())
                    .orElseThrow(() -> new BusinessException("Variant not found: " + req.sku()));
            v.reserveStock(req.quantity());   // PA-07: throw if insufficient
        }
        return ApiResponse.ok(null);
    }

    @PutMapping("/variants/release")
    @Transactional
    public ApiResponse<Void> releaseStock(@RequestBody List<StockRequest> requests) {
        for (StockRequest req : requests) {
            variantRepository.findBySkuIgnoreCase(req.sku())
                    .ifPresent(v -> v.releaseStock(req.quantity()));
        }
        return ApiResponse.ok(null);
    }

    record VariantSnapshotDto(UUID productId, UUID variantId, String sku,
                               String productName, String primaryImage,
                               String size, String color, BigDecimal price,
                               int stock, boolean active) {}

    record StockRequest(String sku, int quantity) {}
}
