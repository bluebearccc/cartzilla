package com.cartzilla.order.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Bao response phân trang chuẩn cho list endpoint (F10). */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
