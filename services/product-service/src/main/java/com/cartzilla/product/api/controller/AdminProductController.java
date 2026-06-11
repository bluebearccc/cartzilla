package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.PageResponse;
import com.cartzilla.product.api.dto.ProductDtos.CreateProductRequest;
import com.cartzilla.product.api.dto.ProductDtos.ImageRequest;
import com.cartzilla.product.api.dto.ProductDtos.ProductDetailResponse;
import com.cartzilla.product.api.dto.ProductDtos.ProductSummaryResponse;
import com.cartzilla.product.api.dto.ProductDtos.UpdateProductRequest;
import com.cartzilla.product.api.dto.ProductDtos.UpdateVariantRequest;
import com.cartzilla.product.api.dto.ProductDtos.VariantRequest;
import com.cartzilla.product.application.usecase.CreateProductUseCase;
import com.cartzilla.product.application.usecase.DeleteProductUseCase;
import com.cartzilla.product.application.usecase.GetProductUseCase;
import com.cartzilla.product.application.usecase.ListProductsUseCase;
import com.cartzilla.product.application.usecase.ManageImageUseCase;
import com.cartzilla.product.application.usecase.ManageVariantUseCase;
import com.cartzilla.product.application.usecase.UpdateProductUseCase;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin catalog — F11 (UC-05). Gateway JwtAuth + AdminRoleInterceptor enforce ADMIN.
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_PRODUCTS)
@RequiredArgsConstructor
public class AdminProductController {

    private final ListProductsUseCase listProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ManageVariantUseCase manageVariantUseCase;
    private final ManageImageUseCase manageImageUseCase;

    // ─── Product ───────────────────────────────────────────────────────────

    /** GET /api/admin/products — gồm cả inactive */
    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        ProductSearchCriteria criteria = ProductSearchCriteria.admin(categoryId, keyword);
        Page<Product> result = listProductsUseCase.execute(criteria, page, limit, sort);
        return ApiResponse.ok(PageResponse.from(result.map(ProductSummaryResponse::from)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable UUID id) {
        return ApiResponse.ok(ProductDetailResponse.from(getProductUseCase.executeForAdmin(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(
            @Valid @RequestBody CreateProductRequest req) {
        Product product = createProductUseCase.execute(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo sản phẩm thành công", ProductDetailResponse.from(product)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateProductRequest req) {
        Product product = updateProductUseCase.execute(id, req.toCommand());
        return ApiResponse.ok("Cập nhật sản phẩm thành công", ProductDetailResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        deleteProductUseCase.execute(id);
        return ApiResponse.ok("Đã xóa sản phẩm", null);
    }

    // ─── Variants (PV-01..PV-05) ───────────────────────────────────────────

    @PostMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> addVariant(
            @PathVariable UUID id, @Valid @RequestBody VariantRequest req) {
        Product product = manageVariantUseCase.add(id, req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm variant thành công", ProductDetailResponse.from(product)));
    }

    @PutMapping("/{id}/variants/{variantId}")
    public ApiResponse<ProductDetailResponse> updateVariant(
            @PathVariable UUID id, @PathVariable UUID variantId,
            @Valid @RequestBody UpdateVariantRequest req) {
        Product product = manageVariantUseCase.update(id, variantId, req.toCommand());
        return ApiResponse.ok("Cập nhật variant thành công", ProductDetailResponse.from(product));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    public ApiResponse<ProductDetailResponse> deleteVariant(
            @PathVariable UUID id, @PathVariable UUID variantId) {
        Product product = manageVariantUseCase.remove(id, variantId);
        return ApiResponse.ok("Đã xóa variant", ProductDetailResponse.from(product));
    }

    // ─── Images (PI-01..PI-03, PA-05) ──────────────────────────────────────

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> addImage(
            @PathVariable UUID id, @Valid @RequestBody ImageRequest req) {
        Product product = manageImageUseCase.add(id, req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm ảnh thành công", ProductDetailResponse.from(product)));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ApiResponse<ProductDetailResponse> setPrimaryImage(
            @PathVariable UUID id, @PathVariable UUID imageId) {
        Product product = manageImageUseCase.setPrimary(id, imageId);
        return ApiResponse.ok("Đã đặt ảnh primary", ProductDetailResponse.from(product));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<ProductDetailResponse> deleteImage(
            @PathVariable UUID id, @PathVariable UUID imageId) {
        Product product = manageImageUseCase.remove(id, imageId);
        return ApiResponse.ok("Đã xóa ảnh", ProductDetailResponse.from(product));
    }
}
