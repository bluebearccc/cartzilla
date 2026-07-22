package com.cartzilla.product.application.usecase;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.entity.StockReservation;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.product.domain.vo.StockReservationStatus;
import com.cartzilla.product.infrastructure.persistence.StockReservationJpaRepository;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveStockUseCaseTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private StockReservationJpaRepository reservationRepository;

    @InjectMocks
    private ReserveStockUseCase reserveStockUseCase;

    private UUID orderId;
    private ProductVariant variant1;
    private ProductVariant variant2;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        variant1 = ProductVariant.create("SKU-001", "M", "Red", "#FF0000", new BigDecimal("100.00"), 10);
        variant2 = ProductVariant.create("SKU-002", "L", "Blue", "#0000FF", new BigDecimal("120.00"), 5);
    }

    @Test
    @DisplayName("Saga Reserve Stock thành công khi đủ số lượng kho")
    void reserve_saga_success() {
        StockReservation reservation = new StockReservation(orderId);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));
        when(variantRepository.findBySkuForUpdate("SKU-002")).thenReturn(Optional.of(variant2));

        StockEvents.StockReserveEvent event = new StockEvents.StockReserveEvent(
                orderId, List.of(
                new StockEvents.Item("SKU-002", 2),
                new StockEvents.Item("SKU-001", 3)
        ));

        String failedSku = reserveStockUseCase.reserve(event);

        assertNull(failedSku);
        assertEquals(7, variant1.getStock());
        assertEquals(3, variant2.getStock());
        assertEquals(StockReservationStatus.RESERVED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("Saga Reserve Stock thất bại khi tồn kho không đủ (PA-07)")
    void reserve_saga_insufficientStock() {
        StockReservation reservation = new StockReservation(orderId);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));

        StockEvents.StockReserveEvent event = new StockEvents.StockReserveEvent(
                orderId, List.of(new StockEvents.Item("SKU-001", 15)));

        String failedSku = reserveStockUseCase.reserve(event);

        assertEquals("SKU-001", failedSku);
        assertEquals(10, variant1.getStock());
        assertEquals(StockReservationStatus.FAILED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("Saga Reserve Stock bỏ qua nếu đơn hàng đã được xử lý (Idempotency)")
    void reserve_saga_idempotency() {
        StockReservation reservation = new StockReservation(orderId);
        reservation.markReserved();
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));

        StockEvents.StockReserveEvent event = new StockEvents.StockReserveEvent(
                orderId, List.of(new StockEvents.Item("SKU-001", 1)));

        String result = reserveStockUseCase.reserve(event);

        assertNull(result);
        verify(variantRepository, never()).findBySkuForUpdate(any());
    }

    @Test
    @DisplayName("Saga Release Stock thành công khi hủy đơn hàng")
    void release_saga_success() {
        StockReservation reservation = new StockReservation(orderId);
        reservation.markReserved();
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));

        StockEvents.StockReleaseEvent event = new StockEvents.StockReleaseEvent(
                orderId, List.of(new StockEvents.Item("SKU-001", 3)));

        reserveStockUseCase.release(event);

        assertEquals(13, variant1.getStock());
        assertEquals(StockReservationStatus.RELEASED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("Internal REST Reserve Stock ném BusinessException khi kho không đủ")
    void reserveOrThrow_insufficient_throwsException() {
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));

        List<ReserveStockUseCase.StockLine> lines = List.of(new ReserveStockUseCase.StockLine("SKU-001", 20));

        assertThrows(BusinessException.class, () -> reserveStockUseCase.reserveOrThrow(lines));
        assertEquals(10, variant1.getStock());
    }

    @Test
    @DisplayName("Internal REST Reserve Stock thành công trừ kho đúng số lượng")
    void reserveOrThrow_success() {
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));

        List<ReserveStockUseCase.StockLine> lines = List.of(new ReserveStockUseCase.StockLine("SKU-001", 4));

        reserveStockUseCase.reserveOrThrow(lines);
        assertEquals(6, variant1.getStock());
    }

    @Test
    @DisplayName("Internal REST Release Stock hoàn lại số lượng tồn kho")
    void releaseLines_success() {
        when(variantRepository.findBySkuForUpdate("SKU-001")).thenReturn(Optional.of(variant1));

        List<ReserveStockUseCase.StockLine> lines = List.of(new ReserveStockUseCase.StockLine("SKU-001", 5));

        reserveStockUseCase.releaseLines(lines);
        assertEquals(15, variant1.getStock());
    }
}
