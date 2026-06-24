package com.cartzilla.order.api.controller;

import com.cartzilla.order.application.usecase.GetUserOrderStatsUseCase;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final GetUserOrderStatsUseCase getUserOrderStatsUseCase;

    @GetMapping("/users/{userId}/stats")
    public ApiResponse<UserOrderStatsResponse> getStats(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID excludeOrderId) {
        var stats = getUserOrderStatsUseCase.execute(userId, excludeOrderId);
        return ApiResponse.ok(new UserOrderStatsResponse(
                stats.nonCancelledOrdersCount(),
                stats.completedOrdersCount(),
                stats.totalSpent()
        ));
    }

    public record UserOrderStatsResponse(
            long nonCancelledOrdersCount,
            long completedOrdersCount,
            BigDecimal totalSpent
    ) {}
}
