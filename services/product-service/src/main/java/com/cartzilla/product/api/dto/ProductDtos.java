package com.cartzilla.product.api.dto;

import com.cartzilla.product.application.command.ProductCommand;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Request/Response DTO cho product (kèm map sang command — F03/F04/F11). */
public class ProductDtos {
    private ProductDtos() {}

    // ─── Requests ──────────────────────────────────────────────────────────

    public record VariantRequest(
            @NotBlank @Size(max = 50) String sku,
            @Size(max = 20) String size,
            @Size(max = 50) String color,
            @Size(max = 10) String colorHex,
            @NotNull @DecimalMin("0") BigDecimal price,
            @Min(0) int stock) {
        public ProductCommand.VariantData toCommand() {
            return new ProductCommand.VariantData(sku, size, color, colorHex, price, stock);
        }
    }

    public record UpdateVariantRequest(
            @Size(max = 20) String size,
            @Size(max = 50) String color,
            @Size(max = 10) String colorHex,
            @NotNull @DecimalMin("0") BigDecimal price,
            @Min(0) int stock) {
        public ProductCommand.UpdateVariant toCommand() {
            return new ProductCommand.UpdateVariant(size, color, colorHex, price, stock);
        }
    }

    public record ImageRequest(
            @NotBlank String imageUrl,
            @Size(max = 200) String altText,
            boolean isPrimary,
            @Min(0) int sortOrder) {
        public ProductCommand.ImageData toCommand() {
            return new ProductCommand.ImageData(imageUrl, altText, isPrimary, sortOrder);
        }
    }

    public record CreateProductRequest(
            @NotNull UUID categoryId,
            UUID vendorId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 220) String slug,
            String description,
            @NotNull @DecimalMin("0") BigDecimal basePrice,
            String tags,
            @Valid List<VariantRequest> variants,
            @Valid List<ImageRequest> images) {
        public ProductCommand.Create toCommand() {
            return new ProductCommand.Create(categoryId, vendorId, name, slug, description,
                    basePrice, tags,
                    variants == null ? List.of() : variants.stream().map(VariantRequest::toCommand).toList(),
                    images == null ? List.of() : images.stream().map(ImageRequest::toCommand).toList());
        }
    }

    public record UpdateProductRequest(
            @NotNull UUID categoryId,
            UUID vendorId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 220) String slug,
            String description,
            @NotNull @DecimalMin("0") BigDecimal basePrice,
            String tags,
            Boolean active,
            Boolean featured) {
        public ProductCommand.Update toCommand() {
            return new ProductCommand.Update(categoryId, vendorId, name, slug, description,
                    basePrice, tags, active, featured);
        }
    }

    // ─── Responses ─────────────────────────────────────────────────────────

    public record VariantResponse(UUID id, String sku, String size, String color,
                                  String colorHex, BigDecimal price, int stock) {
        public static VariantResponse from(ProductVariant v) {
            return new VariantResponse(v.getId(), v.getSku(), v.getSize(), v.getColor(),
                    v.getColorHex(), v.getPrice(), v.getStock());
        }
    }

    public record ImageResponse(UUID id, String imageUrl, String altText,
                                boolean isPrimary, int sortOrder) {
        public static ImageResponse from(ProductImage i) {
            return new ImageResponse(i.getId(), i.getImageUrl(), i.getAltText(),
                    i.isPrimary(), i.getSortOrder());
        }
    }

    /** Item cho list/grid (F03). */
    public record ProductSummaryResponse(
            UUID id, String name, String slug, BigDecimal basePrice, String primaryImage,
            UUID categoryId, UUID vendorId, String tags,
            boolean active, boolean featured, boolean inStock, int totalStock) {
        public static ProductSummaryResponse from(Product p) {
            return new ProductSummaryResponse(p.getId(), p.getName(), p.getSlug(),
                    p.getBasePrice(), primaryImageOf(p), p.getCategoryId(), p.getVendorId(),
                    p.getTags(), p.isActive(), p.isFeatured(), hasStock(p), totalStockOf(p));
        }
    }

    /** Chi tiết product với variants + images (F04). */
    public record ProductDetailResponse(
            UUID id, String name, String slug, String description, BigDecimal basePrice,
            UUID categoryId, UUID vendorId, String tags,
            boolean active, boolean featured, boolean sellable,
            List<VariantResponse> variants, List<ImageResponse> images, Instant createdAt) {
        public static ProductDetailResponse from(Product p) {
            List<VariantResponse> variants = p.getVariants().stream()
                    .filter(v -> !v.isDeleted())
                    .map(VariantResponse::from)
                    .toList();
            List<ImageResponse> images = p.getImages().stream()
                    .filter(i -> !i.isDeleted())
                    .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                    .map(ImageResponse::from)
                    .toList();
            return new ProductDetailResponse(p.getId(), p.getName(), p.getSlug(),
                    p.getDescription(), p.getBasePrice(), p.getCategoryId(), p.getVendorId(),
                    p.getTags(), p.isActive(), p.isFeatured(), p.isSellable(),
                    variants, images, p.getCreatedAt());
        }
    }

    private static String primaryImageOf(Product p) {
        return p.getImages().stream()
                .filter(i -> !i.isDeleted() && i.isPrimary())
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getImages().stream()
                        .filter(i -> !i.isDeleted())
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }

    private static boolean hasStock(Product p) {
        return p.getVariants().stream().anyMatch(v -> !v.isDeleted() && v.getStock() > 0);
    }

    private static int totalStockOf(Product p) {
        return p.getVariants().stream()
                .filter(v -> !v.isDeleted())
                .mapToInt(ProductVariant::getStock)
                .sum();
    }
}
