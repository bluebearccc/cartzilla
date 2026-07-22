package com.cartzilla.product.domain.entity;

import com.cartzilla.product.domain.vo.StockReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {
    @Id @Column(name = "order_id") private UUID orderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StockReservationStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
    public StockReservation(UUID orderId) { this.orderId = orderId; this.status = StockReservationStatus.PENDING; this.createdAt = Instant.now(); }
    public void markReserved() { status = StockReservationStatus.RESERVED; updatedAt = Instant.now(); }
    public void markFailed() { status = StockReservationStatus.FAILED; updatedAt = Instant.now(); }
    public void markReleased() { status = StockReservationStatus.RELEASED; updatedAt = Instant.now(); }
}
