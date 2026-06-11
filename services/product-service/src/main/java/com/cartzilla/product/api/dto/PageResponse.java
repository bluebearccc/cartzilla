package com.cartzilla.product.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envelope phân trang chuẩn cho list API (NFR: search/list có phân trang). */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
