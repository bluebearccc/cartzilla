package com.cartzilla.order.api.controller;

import com.cartzilla.order.api.dto.CartDtos;
import com.cartzilla.order.application.command.CartCommand;
import com.cartzilla.order.application.usecase.ManageCartUseCase;
import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** F05 — UC-03: giỏ hàng của Customer. userId lấy từ header X-User-Id (gateway inject). */
@RestController
@RequestMapping("/api/orders/cart")
@RequiredArgsConstructor
public class CartController {

    private final ManageCartUseCase manageCartUseCase;

    @GetMapping("/items")
    public ApiResponse<CartDtos.CartResponse> view(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(CartDtos.CartResponse.from(manageCartUseCase.view(userId)));
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.CartItemResponse> add(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CartDtos.AddCartItemRequest req) {
        CartItem item = manageCartUseCase.addItem(
                new CartCommand.AddItem(userId, req.sku(), req.quantity()));
        return ApiResponse.ok("Đã thêm vào giỏ", CartDtos.CartItemResponse.from(item));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<Void> update(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID itemId,
            @Valid @RequestBody CartDtos.UpdateCartItemRequest req) {
        manageCartUseCase.updateQuantity(new CartCommand.UpdateItem(userId, itemId, req.quantity()));
        return ApiResponse.ok("Đã cập nhật giỏ hàng", null);
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> remove(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID itemId) {
        manageCartUseCase.removeItem(userId, itemId);
        return ApiResponse.ok("Đã xóa khỏi giỏ", null);
    }
}
