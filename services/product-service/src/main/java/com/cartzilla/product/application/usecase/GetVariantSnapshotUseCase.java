package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cung cấp variant snapshot (price/stock/name/image) cho order-service qua Feign:
 * tạo CartItem snapshot (CTA-05), refresh price khi checkout (CTA-03),
 * validate stock trước khi tạo order (PA-07, X-01).
 * Snapshot được materialize trong transaction để tránh LazyInitializationException.
 */
@Service
@RequiredArgsConstructor
public class GetVariantSnapshotUseCase {

    private final ProductVariantRepository variantRepository;

    public record VariantSnapshot(UUID productId, UUID variantId, String sku,
                                  String productName, String primaryImage,
                                  String size, String color, BigDecimal price,
                                  int stock, boolean active) {}

    @Transactional(readOnly = true)
    public VariantSnapshot execute(String sku) {
        ProductVariant v = variantRepository.findBySku(sku)
                .orElseThrow(() -> new BusinessException("Variant not found: " + sku));
        Product product = v.getProduct();
        String primaryImage = product.getImages().stream()
                .filter(i -> !i.isDeleted() && i.isPrimary())
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .filter(i -> !i.isDeleted())
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
        return new VariantSnapshot(
                product.getId(), v.getId(), v.getSku(),
                product.getName(), primaryImage,
                v.getSize(), v.getColor(), v.getPrice(),
                v.getStock(), product.isActive() && !v.isDeleted());
    }
}
