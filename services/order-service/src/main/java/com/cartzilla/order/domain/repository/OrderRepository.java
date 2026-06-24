package com.cartzilla.order.domain.repository;

import com.cartzilla.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cartzilla.order.domain.vo.UserOrderStats;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    List<Order> findByUserId(UUID userId);

    /** F10: staff list + filter (status/payment/date) có phân trang. */
    Page<Order> search(OrderSearchCriteria criteria, Pageable pageable);

    UserOrderStats getUserOrderStats(UUID userId, UUID excludeOrderId);
}
