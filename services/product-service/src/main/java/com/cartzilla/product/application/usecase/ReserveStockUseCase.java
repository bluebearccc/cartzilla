package com.cartzilla.product.application.usecase;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.infrastructure.persistence.ProductVariantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trừ / hoàn tồn kho khi nhận stock.reserve / stock.release event. */
@Service
@RequiredArgsConstructor
public class ReserveStockUseCase {

    private final ProductVariantJpaRepository variantRepository;

    /**
     * Reserve stock cho tất cả SKU trong event.
     * PA-07: từ chối nếu stock không đủ.
     * @return null nếu thành công, SKU bị thiếu nếu thất bại.
     */
    @Transactional
    public String reserve(StockEvents.StockReserveEvent event) {
        // Bước 1: validate đủ stock trước khi trừ bất kỳ cái nào
        for (StockEvents.Item item : event.items()) {
            ProductVariant v = variantRepository.findBySkuIgnoreCase(item.sku()).orElse(null);
            if (v == null || v.isDeleted()) return item.sku();
            if (v.getStock() < item.quantity()) return item.sku();
        }
        // Bước 2: trừ kho — PA-07 domain method enforce PV-03
        for (StockEvents.Item item : event.items()) {
            variantRepository.findBySkuIgnoreCase(item.sku())
                    .ifPresent(v -> v.reserveStock(item.quantity()));
        }
        return null;
    }

    @Transactional
    public void release(StockEvents.StockReleaseEvent event) {
        for (StockEvents.Item item : event.items()) {
            variantRepository.findBySkuIgnoreCase(item.sku())
                    .ifPresent(v -> v.releaseStock(item.quantity()));
        }
    }
}
