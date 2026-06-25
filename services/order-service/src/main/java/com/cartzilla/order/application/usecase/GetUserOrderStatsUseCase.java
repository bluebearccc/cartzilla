package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.UserOrderStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserOrderStatsUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public UserOrderStats execute(UUID userId, UUID excludeOrderId) {
        return orderRepository.getUserOrderStats(userId, excludeOrderId);
    }
}
