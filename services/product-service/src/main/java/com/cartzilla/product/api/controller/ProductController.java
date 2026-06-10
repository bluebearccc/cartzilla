package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.PageResponse;
import com.cartzilla.product.api.dto.ProductDtos.ProductDetailResponse;
import com.cartzilla.product.api.dto.ProductDtos.ProductSummaryResponse;
import com.cartzilla.product.application.usecase.GetProductUseCase;
import com.cartzilla.product.application.usecase.ListProductsUseCase;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Public catalog — F03 (browse/search/filter/sort/page) + F04 (detail). */
@RestController
@RequestMapping(ApiPaths.PRODUCTS)
@RequiredArgsConstructor
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final GetProductUseCase getProductUseCase;

    /** GET /api/products?category=ao&size=M&color=&vendorId=&minPrice=&maxPrice=&q=&sort=&page=&limit= */
    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(name = "category", required = false) String categorySlug,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "12") int limit) {
        ProductSearchCriteria criteria = ProductSearchCriteria.publicCatalog(
                categoryId, categorySlug, keyword, size, color,
                vendorId, minPrice, maxPrice, inStock, featured);
        Page<Product> result = listProductsUseCase.execute(criteria, page, limit, sort);
        return ApiResponse.ok(PageResponse.from(result.map(ProductSummaryResponse::from)));
    }

    /** GET /api/products/{id} — F04 */
    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable UUID id) {
        return ApiResponse.ok(ProductDetailResponse.from(getProductUseCase.execute(id)));
    }

    /** GET /api/products/slug/{slug} — detail theo SEO slug */
    @GetMapping("/slug/{slug}")
    public ApiResponse<ProductDetailResponse> detailBySlug(@PathVariable String slug) {
        return ApiResponse.ok(ProductDetailResponse.from(getProductUseCase.executeBySlug(slug)));
    }
}
