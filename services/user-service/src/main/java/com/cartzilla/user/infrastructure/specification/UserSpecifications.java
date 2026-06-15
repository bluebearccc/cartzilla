package com.cartzilla.user.infrastructure.specification;

import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {}

    public static Specification<User> from(UserSearchCriteria criteria) {
        return keywordContains(criteria.normalizedKeyword())
                .and(roleEquals(criteria))
                .and(activeEquals(criteria));
    }

    private static Specification<User> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern));
        };
    }

    private static Specification<User> roleEquals(UserSearchCriteria criteria) {
        return (root, query, cb) -> criteria.role() == null
                ? cb.conjunction()
                : cb.equal(root.get("role"), criteria.role());
    }

    private static Specification<User> activeEquals(UserSearchCriteria criteria) {
        return (root, query, cb) -> criteria.active() == null
                ? cb.conjunction()
                : cb.equal(root.get("active"), criteria.active());
    }
}
