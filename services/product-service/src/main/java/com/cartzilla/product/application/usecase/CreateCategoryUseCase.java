package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.CategoryCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F11 — UC-05: admin tạo category (CA-01, C-01..C-02). */
@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category execute(CategoryCommand.Create cmd) {
        if (cmd.parentId() != null) {
            categoryRepository.findById(cmd.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + cmd.parentId()));
        }
        Slug slug = (cmd.slug() == null || cmd.slug().isBlank())
                ? Slug.fromName(cmd.name(), 120)
                : Slug.of(cmd.slug(), 120);
        if (categoryRepository.existsBySlug(slug.getValue()))
            throw new BusinessException("Category slug already exists (CA-01): " + slug);

        Category category = Category.create(cmd.name(), slug.getValue(), cmd.parentId(),
                cmd.imageUrl(), cmd.sortOrder());
        return categoryRepository.save(category);
    }
}
