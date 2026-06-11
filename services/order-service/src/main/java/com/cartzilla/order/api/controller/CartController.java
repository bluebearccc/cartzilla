package com.cartzilla.order.api.controller;

import com.cartzilla.order.api.dto.CartDtos.AddToCartRequest;
import com.cartzilla.order.api.dto.CartDtos.CartItemResponse;
import com.cartzilla.order.api.dto.CartDtos.CartResponse;
import com.cartzilla.order.api.dto.CartDtos.UpdateCartItemRequest;
import com.cartzilla.order.application.usecase.AddToCartUseCase;
import com.cartzilla.order.application.usecase.ClearCartUseCase;
import com.cartzilla.order.application.usecase.GetCartUseCase;
import com.cartzilla.order.application.usecase.RemoveCartItemUseCase;
import com.cartzilla.order.application.usecase.UpdateCartItemUseCase;
import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/cart")
@RequiredArgsConstructor
public class CartController {

    private final AddToCartUseCase addToCartUseCase;
    private final GetCartUseCase getCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final ClearCartUseCase clearCartUseCase;

    /** POST /api/orders/cart — thêm sản phẩm vào giỏ */
    @PostMapping
    public ApiResponse<CartItemResponse> addToCart(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AddToCartRequest request) {

        CartItem item = addToCartUseCase.execute(
                userId, request.sku(), request.quantity());
        return ApiResponse.ok("Đã thêm vào giỏ hàng", CartItemResponse.from(item));
    }

    /** GET /api/orders/cart — xem giỏ hàng */
    @GetMapping
    public ApiResponse<CartResponse> getCart(
            @RequestHeader("X-User-Id") UUID userId) {

        var items = getCartUseCase.execute(userId);
        return ApiResponse.ok(CartResponse.from(items));
    }

    /** PUT /api/orders/cart/{sku} — cập nhật số lượng */
    @PutMapping("/{sku}")
    public ApiResponse<CartItemResponse> updateCartItem(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String sku,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartItem item = updateCartItemUseCase.execute(
                userId, sku, request.quantity());
        if (item == null) {
            return ApiResponse.ok("Đã xóa khỏi giỏ hàng", null);
        }
        return ApiResponse.ok("Đã cập nhật giỏ hàng", CartItemResponse.from(item));
    }

    /** DELETE /api/orders/cart/{sku} — xóa 1 item */
    @DeleteMapping("/{sku}")
    public ApiResponse<Void> removeCartItem(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String sku) {

        removeCartItemUseCase.execute(userId, sku);
        return ApiResponse.ok("Đã xóa khỏi giỏ hàng", null);
    }

    /** DELETE /api/orders/cart — xóa toàn bộ giỏ hàng */
    @DeleteMapping
    public ApiResponse<Void> clearCart(
            @RequestHeader("X-User-Id") UUID userId) {

        clearCartUseCase.execute(userId);
        return ApiResponse.ok("Đã xóa toàn bộ giỏ hàng", null);
    }
}
