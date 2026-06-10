package com.cartzilla.order.domain.entity;

import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Child entity của OrderAggregate — append-only audit log.
 * Rules: OSL-01..OSL-04, OA-08.
 */
@Entity
@Table(name = "order_status_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** OSL-02: nullable cho log đầu tiên */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    /** OSL-01: NOT NULL */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    /** OSL-04: nullable nếu system/saga */
    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** Factory — OSL-03: không update/delete */
    static OrderStatusLog create(Order order, OrderStatus from, OrderStatus to,
                                  UUID changedBy, String note) {
        if (to == null) throw new BusinessException("toStatus must not be null (OSL-01)");
        OrderStatusLog log = new OrderStatusLog();
        log.order = order;
        log.fromStatus = from;
        log.toStatus = to;
        log.changedBy = changedBy;
        log.note = note;
        return log;
    }
}
