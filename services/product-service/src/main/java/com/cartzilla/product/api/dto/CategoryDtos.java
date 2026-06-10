package com.cartzilla.product.api.dto;

import com.cartzilla.product.application.command.CategoryCommand;
import com.cartzilla.product.domain.entity.Category;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Request/Response DTO cho category (kèm map sang command — F11). */
public class CategoryDtos {
    private CategoryDtos() {}

    // ─── Requests ──────────────────────────────────────────────────────────

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 120) String slug,
            UUID parentId,
            String imageUrl,
            @Min(0) int sortOrder) {
        public CategoryCommand.Create toCommand() {
            return new CategoryCommand.Create(name, slug, parentId, imageUrl, sortOrder);
        }
    }

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 120) String slug,
            UUID parentId,
            String imageUrl,
            @Min(0) int sortOrder,
            Boolean active) {
        public CategoryCommand.Update toCommand() {
            return new CategoryCommand.Update(name, slug, parentId, imageUrl, sortOrder, active);
        }
    }

    // ─── Responses ─────────────────────────────────────────────────────────

    public record CategoryResponse(
            UUID id, UUID parentId, String name, String slug, String imageUrl,
            boolean active, int sortOrder, List<CategoryResponse> children) {

        public static CategoryResponse from(Category c) {
            return from(c, List.of());
        }

        public static CategoryResponse from(Category c, List<CategoryResponse> children) {
            return new CategoryResponse(c.getId(), c.getParentId(), c.getName(), c.getSlug(),
                    c.getImageUrl(), c.isActive(), c.getSortOrder(), children);
        }
    }

    /** Build cây cha-con từ flat list (đã sort theo sortOrder). */
    public static List<CategoryResponse> buildTree(List<Category> all) {
        Map<UUID, List<Category>> byParent = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toNode(c, byParent))
                .toList();
    }

    private static CategoryResponse toNode(Category c, Map<UUID, List<Category>> byParent) {
        List<CategoryResponse> children = byParent.getOrDefault(c.getId(), List.of()).stream()
                .map(child -> toNode(child, byParent))
                .toList();
        return CategoryResponse.from(c, children);
    }
}
