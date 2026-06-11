package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.product.api.dto.CategoryDtos;
import com.cartzilla.product.api.dto.CategoryDtos.CategoryResponse;
import com.cartzilla.product.application.usecase.ListCategoriesUseCase;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public category tree — UC-01 (menu/filter sidebar). */
@RestController
@RequestMapping(ApiPaths.CATEGORIES)
@RequiredArgsConstructor
public class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;

    /** GET /api/categories — cây category active */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> tree() {
        return ApiResponse.ok(CategoryDtos.buildTree(listCategoriesUseCase.execute(false)));
    }
}
