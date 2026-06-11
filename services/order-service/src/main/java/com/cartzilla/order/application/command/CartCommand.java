package com.cartzilla.order.application.command;

import java.util.UUID;

/** Input command cho thao tác giỏ hàng (F05 / UC-03). */
public class CartCommand {
    private CartCommand() {}

    public record AddItem(UUID userId, String sku, int quantity) {}

    public record UpdateItem(UUID userId, UUID itemId, int quantity) {}
}
