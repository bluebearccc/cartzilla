package com.cartzilla.product.application.usecase;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.product.domain.entity.StockReservation;
import com.cartzilla.product.domain.vo.StockReservationStatus;
import com.cartzilla.product.infrastructure.persistence.StockReservationJpaRepository;
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
    private final StockReservationJpaRepository reservationRepository;

    public record StockLine(String sku, int quantity) {}

    // ─── Saga event path (không throw — trả failedSku để publish StockReservedEvent) ───

    /**
     * Reserve stock cho tất cả SKU trong event.
     * PA-07: từ chối nếu stock không đủ.
     * @return null nếu thành công, SKU đầu tiên bị thiếu nếu thất bại.
     */
    @Transactional
    public String reserve(StockEvents.StockReserveEvent event) {
        reservationRepository.insertIfAbsent(event.orderId());
        StockReservation reservation = reservationRepository.findByOrderId(event.orderId()).orElseThrow();
        if (reservation.getStatus() == StockReservationStatus.RESERVED
                || reservation.getStatus() == StockReservationStatus.RELEASED) return null;
        List<StockEvents.Item> items = event.items().stream()
                .sorted(java.util.Comparator.comparing(StockEvents.Item::sku)).toList();
        // Lock in a stable SKU order to avoid deadlocks between concurrent orders.
        for (StockEvents.Item item : items) {
            ProductVariant v = variantRepository.findBySkuForUpdate(item.sku()).orElse(null);
            if (v == null || v.getStock() < item.quantity()) {
                reservation.markFailed();
                reservationRepository.save(reservation);
                return item.sku();
            }
        }
        // Bước 2: trừ kho — domain method enforce PV-03
        for (StockEvents.Item item : items) {
            variantRepository.findBySkuForUpdate(item.sku())
                    .ifPresent(v -> v.reserveStock(item.quantity()));
        }
        reservation.markReserved();
        reservationRepository.save(reservation);
        return null;
    }

    @Transactional
    public void release(StockEvents.StockReleaseEvent event) {
        StockReservation reservation = reservationRepository.findByOrderId(event.orderId()).orElse(null);
        if (reservation == null || reservation.getStatus() != StockReservationStatus.RESERVED) return;
        releaseLines(event.items().stream()
                .map(i -> new StockLine(i.sku(), i.quantity())).toList());
        reservation.markReleased();
        reservationRepository.save(reservation);
    }

    // ─── Internal REST path (Feign từ order-service — throw nếu thiếu) ───

    /** PA-07/BR-P04: reserve, throw BusinessException nếu SKU không tồn tại hoặc thiếu hàng. */
    @Transactional
    public void reserveOrThrow(List<StockLine> lines) {
        for (StockLine line : lines.stream().sorted(java.util.Comparator.comparing(StockLine::sku)).toList()) {
            ProductVariant v = variantRepository.findBySkuForUpdate(line.sku())
                    .orElseThrow(() -> new BusinessException("Variant not found: " + line.sku()));
            v.reserveStock(line.quantity()); // PA-07: throw if insufficient
        }
    }

    /** X-04 compensation: hoàn trả tồn kho; bỏ qua SKU không còn tồn tại. */
    @Transactional
    public void releaseLines(List<StockLine> lines) {
        for (StockLine line : lines.stream().sorted(java.util.Comparator.comparing(StockLine::sku)).toList()) {
            variantRepository.findBySkuForUpdate(line.sku())
                    .ifPresent(v -> v.releaseStock(line.quantity()));
        }
    }
}
