package com.cartzilla.order.api.dto;

import com.cartzilla.order.domain.entity.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request/Response DTO cho giỏ hàng (F05). */
public class CartDtos {
    private CartDtos() {}

    public record AddCartItemRequest(@NotBlank String sku, @Min(1) int quantity) {}

    /** quantity = 0 → xóa dòng cart (BR-O02). */
    public record UpdateCartItemRequest(@Min(0) int quantity) {}

    public record CartItemResponse(
            UUID id, UUID productId, String sku, String name, String image,
            String size, String color, BigDecimal price, int quantity, BigDecimal subtotal) {
        public static CartItemResponse from(CartItem c) {
            return new CartItemResponse(c.getId(), c.getProductId(), c.getSku(), c.getName(),
                    c.getImage(), c.getSize(), c.getColor(), c.getPrice(), c.getQuantity(),
                    c.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
    }

    public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal subtotal) {
        public static CartResponse from(List<CartItem> items) {
            List<CartItemResponse> lines = items.stream().map(CartItemResponse::from).toList();
            int totalQty = lines.stream().mapToInt(CartItemResponse::quantity).sum();
            BigDecimal subtotal = lines.stream()
                    .map(CartItemResponse::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(lines, totalQty, subtotal);
        }
    }
}
