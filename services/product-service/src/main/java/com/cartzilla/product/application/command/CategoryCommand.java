package com.cartzilla.product.application.command;

import java.util.UUID;

/** Input commands cho category use cases (F11 — UC-05). */
public class CategoryCommand {
    private CategoryCommand() {}

    public record Create(
            String name,
            String slug,
            UUID parentId,
            String imageUrl,
            int sortOrder) {}

    public record Update(
            String name,
            String slug,
            UUID parentId,
            String imageUrl,
            int sortOrder,
            Boolean active) {}
}
