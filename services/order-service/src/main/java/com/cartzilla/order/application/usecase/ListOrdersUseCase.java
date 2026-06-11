package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.OrderSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F10 — UC-04: staff/admin list + filter đơn hàng, mới nhất trước. */
@Service
@RequiredArgsConstructor
public class ListOrdersUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<Order> execute(OrderSearchCriteria criteria, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return orderRepository.search(criteria,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"))));
    }
}
