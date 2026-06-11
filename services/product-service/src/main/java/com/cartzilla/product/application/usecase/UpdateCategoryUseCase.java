package com.cartzilla.product.application.usecase;

import com.cartzilla.product.application.command.CategoryCommand;
import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.exception.ResourceNotFoundException;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.vo.Slug;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/** F11 — UC-05: admin cập nhật category (CA-02..CA-05, BR-P07). */
@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private static final int MAX_TREE_DEPTH = 50;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Category execute(UUID id, CategoryCommand.Update cmd) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        category.update(cmd.name(), cmd.imageUrl(), cmd.sortOrder());

        if (cmd.slug() != null && !cmd.slug().isBlank()) {
            Slug slug = Slug.of(cmd.slug(), 120);
            if (!slug.getValue().equals(category.getSlug())) {
                if (categoryRepository.existsBySlug(slug.getValue()))
                    throw new BusinessException("Category slug already exists (CA-01): " + slug);
                category.changeSlug(slug.getValue());
            }
        }

        if (!Objects.equals(cmd.parentId(), category.getParentId())) {
            if (cmd.parentId() != null) {
                categoryRepository.findById(cmd.parentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + cmd.parentId()));
                assertNoCycle(id, cmd.parentId()); // CA-03
            }
            category.setParent(cmd.parentId()); // CA-02 guard self-ref
        }

        if (cmd.active() != null) {
            if (cmd.active()) {
                category.activate();
            } else {
                // CA-04/BR-P07: không deactivate khi còn product active
                if (productRepository.existsActiveByCategoryId(id))
                    throw new BusinessException(
                            "Cannot deactivate category with active products (CA-04/BR-P07)");
                category.deactivate();
            }
        }

        return categoryRepository.save(category);
    }

    /** CA-03: walk lên cây cha — nếu gặp lại chính nó thì có cycle */
    private void assertNoCycle(UUID categoryId, UUID newParentId) {
        UUID cursor = newParentId;
        int depth = 0;
        while (cursor != null && depth++ < MAX_TREE_DEPTH) {
            if (cursor.equals(categoryId))
                throw new BusinessException("Category tree cycle detected (CA-03)");
            cursor = categoryRepository.findById(cursor).map(Category::getParentId).orElse(null);
        }
    }
}
