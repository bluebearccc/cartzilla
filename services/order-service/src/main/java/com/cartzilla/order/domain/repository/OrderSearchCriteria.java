package com.cartzilla.order.domain.repository;

import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;

import java.time.Instant;

/**
 * Query object cho OrderRepository.search — filter staff orders (F10 / UC-04).
 * Mọi trường null = không lọc.
 */
public record OrderSearchCriteria(
        OrderStatus status,
        PaymentMethod paymentMethod,
        Instant fromDate,
        Instant toDate) {
}
