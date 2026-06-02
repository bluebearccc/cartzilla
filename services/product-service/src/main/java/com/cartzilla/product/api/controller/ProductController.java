package com.cartzilla.product.api.controller;

import com.cartzilla.product.application.usecase.ListProductsUseCase;
import com.cartzilla.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ListProductsUseCase listProductsUseCase;

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return listProductsUseCase.execute(category);
    }
}
