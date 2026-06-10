package com.cartzilla.product.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Input commands cho product use cases (F11 — UC-05). */
public class ProductCommand {
    private ProductCommand() {}

    public record Create(
            UUID categoryId,
            UUID vendorId,
            String name,
            String slug,
            String description,
            BigDecimal basePrice,
            String tags,
            List<VariantData> variants,
            List<ImageData> images) {}

    public record Update(
            UUID categoryId,
            UUID vendorId,
            String name,
            String slug,
            String description,
            BigDecimal basePrice,
            String tags,
            Boolean active,
            Boolean featured) {}

    public record VariantData(
            String sku,
            String size,
            String color,
            String colorHex,
            BigDecimal price,
            int stock) {}

    public record UpdateVariant(
            String size,
            String color,
            String colorHex,
            BigDecimal price,
            int stock) {}

    public record ImageData(
            String imageUrl,
            String altText,
            boolean isPrimary,
            int sortOrder) {}
}
