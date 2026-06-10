package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.CategoryDtos.CategoryResponse;
import com.cartzilla.product.api.dto.CategoryDtos.CreateCategoryRequest;
import com.cartzilla.product.api.dto.CategoryDtos.UpdateCategoryRequest;
import com.cartzilla.product.application.usecase.CreateCategoryUseCase;
import com.cartzilla.product.application.usecase.DeleteCategoryUseCase;
import com.cartzilla.product.application.usecase.ListCategoriesUseCase;
import com.cartzilla.product.application.usecase.UpdateCategoryUseCase;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Admin category — F11 (UC-05): CRUD/deactivate theo cây cha-con. */
@RestController
@RequestMapping(ApiPaths.ADMIN_CATEGORIES)
@RequiredArgsConstructor
public class AdminCategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    /** GET /api/admin/categories — flat list gồm cả inactive */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.ok(listCategoriesUseCase.execute(true).stream()
                .map(CategoryResponse::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest req) {
        var category = createCategoryUseCase.execute(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo category thành công", CategoryResponse.from(category)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateCategoryRequest req) {
        var category = updateCategoryUseCase.execute(id, req.toCommand());
        return ApiResponse.ok("Cập nhật category thành công", CategoryResponse.from(category));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        deleteCategoryUseCase.execute(id);
        return ApiResponse.ok("Đã xóa category", null);
    }
}
