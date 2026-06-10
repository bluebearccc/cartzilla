package com.cartzilla.product.infrastructure.specification;

import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.entity.ProductImage;
import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification cho filter/search product (F03 — UC-01).
 * activeOnly → enforce PA-03 (category active) + BR-P01 (sellable).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> from(ProductSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Boolean.TRUE.equals(c.activeOnly())) {
                predicates.add(cb.isTrue(root.get("active")));

                // PA-03: category phải active
                Subquery<UUID> activeCat = query.subquery(UUID.class);
                Root<Category> cat = activeCat.from(Category.class);
                activeCat.select(cat.get("id")).where(
                        cb.equal(cat.get("id"), root.get("categoryId")),
                        cb.isTrue(cat.get("active")));
                predicates.add(cb.exists(activeCat));

                // BR-P01: sellable = ≥1 variant + ≥1 image
                Subquery<UUID> anyVariant = query.subquery(UUID.class);
                Root<ProductVariant> av = anyVariant.from(ProductVariant.class);
                anyVariant.select(av.get("id")).where(cb.equal(av.get("product"), root));
                predicates.add(cb.exists(anyVariant));

                Subquery<UUID> anyImage = query.subquery(UUID.class);
                Root<ProductImage> ai = anyImage.from(ProductImage.class);
                anyImage.select(ai.get("id")).where(cb.equal(ai.get("product"), root));
                predicates.add(cb.exists(anyImage));
            }

            if (c.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), c.categoryId()));
            }

            if (notBlank(c.categorySlug())) {
                Subquery<UUID> bySlug = query.subquery(UUID.class);
                Root<Category> cat = bySlug.from(Category.class);
                bySlug.select(cat.get("id")).where(
                        cb.equal(cat.get("slug"), c.categorySlug().trim().toLowerCase()),
                        cb.equal(cat.get("id"), root.get("categoryId")));
                predicates.add(cb.exists(bySlug));
            }

            if (c.vendorId() != null) {
                predicates.add(cb.equal(root.get("vendorId"), c.vendorId()));
            }

            if (c.featured() != null) {
                predicates.add(cb.equal(root.get("featured"), c.featured()));
            }

            if (notBlank(c.keyword())) {
                String kw = "%" + c.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), kw),
                        cb.like(cb.lower(root.get("description")), kw),
                        cb.like(cb.lower(root.get("tags")), kw)));
            }

            if (c.minPrice() != null) {
                predicates.add(cb.ge(root.get("basePrice"), c.minPrice()));
            }
            if (c.maxPrice() != null) {
                predicates.add(cb.le(root.get("basePrice"), c.maxPrice()));
            }

            boolean filterVariant = notBlank(c.size()) || notBlank(c.color())
                    || Boolean.TRUE.equals(c.inStockOnly());
            if (filterVariant) {
                // một variant phải thỏa đồng thời size/color/stock
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<ProductVariant> v = sub.from(ProductVariant.class);
                List<Predicate> vp = new ArrayList<>();
                vp.add(cb.equal(v.get("product"), root));
                if (notBlank(c.size())) {
                    vp.add(cb.equal(cb.lower(v.get("size")), c.size().trim().toLowerCase()));
                }
                if (notBlank(c.color())) {
                    vp.add(cb.equal(cb.lower(v.get("color")), c.color().trim().toLowerCase()));
                }
                if (Boolean.TRUE.equals(c.inStockOnly())) {
                    vp.add(cb.greaterThan(v.get("stock"), 0));
                }
                sub.select(v.get("id")).where(vp.toArray(new Predicate[0]));
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
