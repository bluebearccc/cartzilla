package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F11 — UC-05: admin soft-delete category (guard CA-04/BR-P07 + còn category con). */
@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void execute(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        if (productRepository.existsActiveByCategoryId(id))
            throw new BusinessException("Cannot delete category with active products (CA-04/BR-P07)");
        if (categoryRepository.existsByParentId(id))
            throw new BusinessException("Cannot delete category that still has sub-categories");

        category.deactivate();
        category.softDelete();
        categoryRepository.save(category);
    }
}
