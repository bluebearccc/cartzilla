package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.application.usecase.GetVariantSnapshotUseCase;
import com.cartzilla.product.application.usecase.GetVariantSnapshotUseCase.VariantSnapshot;
import com.cartzilla.product.application.usecase.ReserveStockUseCase;
import com.cartzilla.product.application.usecase.ReserveStockUseCase.StockLine;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal endpoints — dùng bởi order-service qua Feign (KHÔNG expose qua gateway public route).
 * - Lấy variant snapshot (CTA-05, PA-06, CTA-03).
 * - Reserve/release stock (PA-07, BR-P04, X-01, X-04).
 * Mọi business logic/transaction nằm trong use case (Hexagonal — controller chỉ điều hướng).
 */
@RestController
@RequestMapping(ApiPaths.INTERNAL_PRODUCTS)
@RequiredArgsConstructor
public class InternalProductController {

    private final GetVariantSnapshotUseCase getVariantSnapshotUseCase;
    private final ReserveStockUseCase reserveStockUseCase;

    @GetMapping("/variants/{sku}")
    public ApiResponse<VariantSnapshot> getVariantBySku(@PathVariable String sku) {
        return ApiResponse.ok(getVariantSnapshotUseCase.execute(sku));
    }

    @PutMapping("/variants/reserve")
    public ApiResponse<Void> reserveStock(@RequestBody @NotEmpty @Valid List<StockRequest> requests) {
        reserveStockUseCase.reserveOrThrow(toLines(requests));
        return ApiResponse.ok(null);
    }

    @PutMapping("/variants/release")
    public ApiResponse<Void> releaseStock(@RequestBody @NotEmpty @Valid List<StockRequest> requests) {
        reserveStockUseCase.releaseLines(toLines(requests));
        return ApiResponse.ok(null);
    }

    private static List<StockLine> toLines(List<StockRequest> requests) {
        return requests.stream().map(r -> new StockLine(r.sku(), r.quantity())).toList();
    }

    public record StockRequest(@NotBlank String sku, @Min(1) int quantity) {}
}
