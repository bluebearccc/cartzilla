package com.cartzilla.order.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Feign client gọi product-service.
 * Dùng Eureka service name: "product-service".
 */
@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductFeignClient {

    /**
     * Lấy snapshot variant (price, stock, name, image) để:
     * - Tạo CartItem snapshot (CTA-05).
     * - Refresh price tại checkout (CTA-03).
     * - Validate stock trước khi tạo order (PA-07, X-01).
     */
    @GetMapping("/api/internal/products/variants/{sku}")
    ApiResponse<VariantSnapshotDto> getVariantBySku(@PathVariable("sku") String sku);

    /**
     * Nội bộ: reserve (giảm) stock cho danh sách SKU sau khi saga RESERVE_STOCK.
     * PA-07, BR-P04.
     */
    @PutMapping("/api/internal/products/variants/reserve")
    ApiResponse<Void> reserveStock(@RequestBody List<StockReserveRequest> requests);

    /**
     * Nội bộ: release (hoàn trả) stock — compensation khi cancel/payment failed.
     * X-04.
     */
    @PutMapping("/api/internal/products/variants/release")
    ApiResponse<Void> releaseStock(@RequestBody List<StockReserveRequest> requests);

    // ─── Inner DTOs ────────────────────────────────────────────────────────

    record VariantSnapshotDto(
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String primaryImage,
            String size,
            String color,
            BigDecimal price,
            int stock,
            boolean active
    ) {}

    record StockReserveRequest(String sku, int quantity) {}
}
