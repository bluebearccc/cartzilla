package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.vo.Role;

public record UserSearchCriteria(String keyword, Role role, Boolean active) {
    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase();
    }
}
