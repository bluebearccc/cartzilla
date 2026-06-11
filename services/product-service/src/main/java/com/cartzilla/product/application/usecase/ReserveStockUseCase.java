package com.cartzilla.product.application.usecase;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trừ / hoàn tồn kho — phục vụ cả Saga (event-based) và internal REST (Feign).
 * Phụ thuộc PORT {@link ProductVariantRepository}, không phụ thuộc JPA trực tiếp (Hexagonal).
 * Rules: PA-07, PV-03, BR-P04, X-04.
 */
@Service
@RequiredArgsConstructor
public class ReserveStockUseCase {

    private final ProductVariantRepository variantRepository;

    public record StockLine(String sku, int quantity) {}

    // ─── Saga event path (không throw — trả failedSku để publish StockReservedEvent) ───

    /**
     * Reserve stock cho tất cả SKU trong event.
     * PA-07: từ chối nếu stock không đủ.
     * @return null nếu thành công, SKU đầu tiên bị thiếu nếu thất bại.
     */
    @Transactional
    public String reserve(StockEvents.StockReserveEvent event) {
        // Bước 1: validate đủ stock cho mọi SKU trước khi trừ bất kỳ cái nào (all-or-nothing)
        for (StockEvents.Item item : event.items()) {
            ProductVariant v = variantRepository.findBySku(item.sku()).orElse(null);
            if (v == null || v.getStock() < item.quantity()) return item.sku();
        }
        // Bước 2: trừ kho — domain method enforce PV-03
        for (StockEvents.Item item : event.items()) {
            variantRepository.findBySku(item.sku())
                    .ifPresent(v -> v.reserveStock(item.quantity()));
        }
        return null;
    }

    @Transactional
    public void release(StockEvents.StockReleaseEvent event) {
        releaseLines(event.items().stream()
                .map(i -> new StockLine(i.sku(), i.quantity())).toList());
    }

    // ─── Internal REST path (Feign từ order-service — throw nếu thiếu) ───

    /** PA-07/BR-P04: reserve, throw BusinessException nếu SKU không tồn tại hoặc thiếu hàng. */
    @Transactional
    public void reserveOrThrow(List<StockLine> lines) {
        for (StockLine line : lines) {
            ProductVariant v = variantRepository.findBySku(line.sku())
                    .orElseThrow(() -> new BusinessException("Variant not found: " + line.sku()));
            v.reserveStock(line.quantity()); // PA-07: throw if insufficient
        }
    }

    /** X-04 compensation: hoàn trả tồn kho; bỏ qua SKU không còn tồn tại. */
    @Transactional
    public void releaseLines(List<StockLine> lines) {
        for (StockLine line : lines) {
            variantRepository.findBySku(line.sku())
                    .ifPresent(v -> v.releaseStock(line.quantity()));
        }
    }
}
