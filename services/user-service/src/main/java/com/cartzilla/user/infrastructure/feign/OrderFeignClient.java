package com.cartzilla.user.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderFeignClient {

    @GetMapping("/api/internal/orders/users/{userId}/stats")
    ApiResponse<UserOrderStatsDto> getUserOrderStats(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "excludeOrderId", required = false) UUID excludeOrderId);

    record UserOrderStatsDto(
            long nonCancelledOrdersCount,
            long completedOrdersCount,
            BigDecimal totalSpent
    ) {}
}
