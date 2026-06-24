package com.cartzilla.order.domain.vo;

import java.math.BigDecimal;

public record UserOrderStats(
        long nonCancelledOrdersCount,
        long completedOrdersCount,
        BigDecimal totalSpent
) {}
