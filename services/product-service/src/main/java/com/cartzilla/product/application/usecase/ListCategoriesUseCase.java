package com.cartzilla.product.application.usecase;

import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC-01/UC-05: danh sách category (public: active; admin: tất cả). */
@Service
@RequiredArgsConstructor
public class ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> execute(boolean includeInactive) {
        return includeInactive ? categoryRepository.findAll() : categoryRepository.findAllActive();
    }
}
