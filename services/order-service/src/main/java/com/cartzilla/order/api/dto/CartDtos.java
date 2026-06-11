package com.cartzilla.order.api.dto;

import com.cartzilla.order.domain.entity.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CartDtos {
    private CartDtos() {}

    // ─── Requests ──────────────────────────────────────────────────────────

    /** POST /api/orders/cart — body */
    public record AddToCartRequest(
            @NotBlank String sku,
            @NotNull @Min(1) Integer quantity
    ) {}

    /** PUT /api/orders/cart/{sku} — body */
    public record UpdateCartItemRequest(
            @NotNull @Min(0) Integer quantity   // quantity = 0 → xóa item
    ) {}

    // ─── Responses ─────────────────────────────────────────────────────────

    public record CartItemResponse(
            UUID id,
            String sku,
            String productName,
            String image,
            String size,
            String color,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
        /** Map từ domain entity sang DTO */
        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.getId(),
                    item.getSku(),
                    item.getName(),
                    item.getImage(),
                    item.getSize(),
                    item.getColor(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }
    }

    public record CartResponse(
            List<CartItemResponse> items,
            BigDecimal total
    ) {
        /** Tính tổng từ danh sách CartItem entity */
        public static CartResponse from(List<CartItem> items) {
            List<CartItemResponse> dtos = items.stream()
                    .map(CartItemResponse::from)
                    .toList();
            BigDecimal total = dtos.stream()
                    .map(CartItemResponse::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(dtos, total);
        }
    }
}
