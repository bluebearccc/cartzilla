package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.StockReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservation, UUID> {
    @Modifying
    @Query(value = "INSERT INTO stock_reservations(order_id,status,created_at) VALUES (:orderId,'PENDING',CURRENT_TIMESTAMP) ON CONFLICT (order_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("orderId") UUID orderId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockReservation> findByOrderId(UUID orderId);
}
