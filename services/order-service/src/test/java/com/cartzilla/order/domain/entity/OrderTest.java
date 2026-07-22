package com.cartzilla.order.domain.entity;

import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.domain.vo.PaymentStatus;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Domain state machine: OA-S1..OA-S5, OA-07, OA-08, BR-O09..BR-O11. */
class OrderTest {

    private static final String ADDR =
            "{\"fullName\":\"A\",\"phone\":\"0900000000\",\"street\":\"1 Lê Lợi\",\"district\":\"Q1\",\"city\":\"HCM\"}";

    private Order codOrder() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "SKU-1", "Áo", null,
                "M", "Đen", new BigDecimal("100000"), 2);
        return Order.create(UUID.randomUUID(), PaymentMethod.COD, ADDR,
                List.of(item), BigDecimal.ZERO, null, null);
    }

    @Test
    void create_computesTotalsAndStartsPending() {
        Order o = codOrder();
        assertEquals(OrderStatus.PENDING, o.getStatus());
        assertEquals(PaymentStatus.PENDING, o.getPaymentStatus());
        assertEquals(new BigDecimal("200000"), o.getSubtotal());
        assertEquals(new BigDecimal("200000"), o.getTotalAmount()); // subtotal - 0
    }

    @Test
    void create_rejectsDiscountExceedingSubtotal_OA03() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "SKU-1", "Áo", null, "M", "Đen",
                new BigDecimal("100000"), 1);
        assertThrows(BusinessException.class, () -> Order.create(UUID.randomUUID(), PaymentMethod.COD,
                ADDR, List.of(item), new BigDecimal("200000"), "V", null));
    }

    @Test
    void happyPath_confirmShipDeliver_writesStatusLogs() {
        Order o = codOrder();
        UUID staff = UUID.randomUUID();
        o.confirm(staff);
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        // BR-PY07: COD giữ PENDING khi CONFIRMED, chỉ PAID khi DELIVERED
        assertEquals(PaymentStatus.PENDING, o.getPaymentStatus());
        assertNotNull(o.getConfirmedAt());

        o.ship(staff);
        assertEquals(OrderStatus.SHIPPING, o.getStatus());

        o.deliver(staff);
        assertEquals(OrderStatus.DELIVERED, o.getStatus());
        assertEquals(PaymentStatus.PAID, o.getPaymentStatus()); // COD delivered → PAID

        assertEquals(3, o.getStatusLogs().size()); // OA-08 append-only
    }

    @Test
    void confirm_vnpayOrder_marksPaid_BRPY07() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "SKU-1", "Áo", null,
                "M", "Đen", new BigDecimal("100000"), 1);
        Order o = Order.create(UUID.randomUUID(), PaymentMethod.VNPAY, ADDR,
                List.of(item), BigDecimal.ZERO, null, null);
        // Saga chỉ confirm đơn VNPay sau callback success → PAID ngay tại confirm
        o.confirm(null);
        assertEquals(PaymentStatus.PAID, o.getPaymentStatus());
    }

    @Test
    void invalidTransition_deliverFromPending_rejected() {
        Order o = codOrder();
        assertThrows(BusinessException.class, () -> o.deliver(UUID.randomUUID()));
    }

    @Test
    void cancel_requiresReason_OA07() {
        Order o = codOrder();
        assertThrows(BusinessException.class, () -> o.cancel("  ", UUID.randomUUID()));
    }

    @Test
    void cancel_terminalStateRejected_OA11() {
        Order o = codOrder();
        UUID staff = UUID.randomUUID();
        o.confirm(staff);
        o.ship(staff);
        o.deliver(staff); // DELIVERED = terminal
        assertThrows(BusinessException.class, () -> o.cancel("đổi ý", staff));
    }

    @Test
    void cancel_shippingOrder_isRejected() {
        Order o = codOrder();
        UUID staff = UUID.randomUUID();
        o.confirm(staff);
        o.ship(staff);
        assertThrows(BusinessException.class, () -> o.cancel("too late", staff));
    }
}
