package com.cartzilla.order.infrastructure.specification;

import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** JPA Specification cho filter staff orders theo status/payment method/khoảng ngày (F10). */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> from(OrderSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.paymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), c.paymentMethod()));
            }
            if (c.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.fromDate()));
            }
            if (c.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), c.toDate()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
