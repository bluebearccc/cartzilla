package com.cartzilla.order.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Bao response phân trang chuẩn cho list endpoint (F10).
 * Field name khớp product-service + frontend PageResponse<T> (items/totalItems)
 * để client đọc đúng danh sách đơn.
 */
public record PageResponse<T>(
        List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
