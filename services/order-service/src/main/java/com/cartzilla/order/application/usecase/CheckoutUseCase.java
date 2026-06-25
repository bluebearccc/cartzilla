package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.OrderItem;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.order.infrastructure.feign.UserFeignClient;
import com.cartzilla.order.infrastructure.saga.OrderSagaOrchestrator;
import com.cartzilla.web.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutUseCase {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID execute(OrderCommand.Checkout cmd) {
        validateCheckout(cmd);
        validateShippingAddress(cmd.shippingAddress());

        // SECURITY (chống thao túng giá): KHÔNG tin unitPrice/name client gửi — luôn dựng lại
        // từng line từ snapshot product-service theo sku + quantity (giá, tồn kho, trạng thái thật).
        var items = cmd.lines().stream()
                .map(line -> toSnapshotLine(new OrderCommand.SkuLine(line.sku(), line.quantity())))
                .map(this::toOrderItem)
                .toList();

        return createOrder(cmd.userId(), items, cmd.shippingAddress(),
                parsePaymentMethod(cmd.paymentMethod()), cmd.voucherCode());
    }

    /** Tạo order + snapshot discount voucher + khởi động saga. Dùng chung cho cả 2 luồng checkout. */
    private UUID createOrder(UUID userId, List<OrderItem> items, String shippingAddress,
                            PaymentMethod paymentMethod, String voucherCode) {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // F14/BR-V10/BR-V13: validate-preview voucher (không tăng usedCount) rồi snapshot discount vào order
        BigDecimal discount = resolveVoucherDiscount(userId, voucherCode, subtotal);

        Order order = Order.create(userId, paymentMethod, shippingAddress, items, discount, voucherCode, null);
        Order saved = orderRepository.save(order);
        sagaOrchestrator.start(saved);
        return saved.getId();
    }

    /**
     * BR-V10: chỉ preview discount, không redeem (redeem sau khi payment thành công trong saga).
     * BR-V12: nếu user nhập voucher nhưng validate lỗi → checkout báo lỗi, KHÔNG âm thầm bỏ voucher.
     */
    private BigDecimal resolveVoucherDiscount(UUID userId, String voucherCode, BigDecimal subtotal) {
        if (voucherCode == null || voucherCode.isBlank()) return BigDecimal.ZERO;
        UserFeignClient.VoucherValidationDto result;
        try {
            var response = userFeignClient.validateVoucher(voucherCode.trim(), userId, subtotal);
            result = response == null ? null : response.data();
            if (response == null || !response.success() || result == null) {
                throw new BusinessException("Không thể xác thực voucher: " + voucherCode);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            // BR-V12: voucher service lỗi → fail-closed
            throw new BusinessException("Không thể xác thực voucher (dịch vụ voucher lỗi): " + voucherCode);
        }
        if (!result.valid()) {
            throw new BusinessException(result.message() == null
                    ? "Voucher không hợp lệ: " + voucherCode : result.message());
        }
        BigDecimal discount = result.discountAmount() == null ? BigDecimal.ZERO : result.discountAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) discount = subtotal; // OA-03 clamp
        return discount;
    }

    @Transactional
    public UUID executeFromSkus(OrderCommand.CheckoutBySku cmd) {
        validateCheckoutBySku(cmd);
        validateShippingAddress(cmd.shippingAddress());

        var items = cmd.lines().stream()
                .map(this::toSnapshotLine)
                .map(this::toOrderItem)
                .toList();

        return createOrder(cmd.userId(), items, cmd.shippingAddress(),
                cmd.paymentMethod(), cmd.voucherCode());
    }

    private OrderCommand.Line toSnapshotLine(OrderCommand.SkuLine line) {
        String sku = requireText(line.sku(), "sku");
        if (line.quantity() <= 0) {
            throw new BusinessException("quantity must be > 0");
        }

        var response = productFeignClient.getVariantBySku(sku);
        var variant = response == null ? null : response.data();
        if (response == null || !response.success() || variant == null) {
            throw new BusinessException("Cannot load product variant for sku=" + sku);
        }
        if (!variant.active()) {
            throw new BusinessException("Variant is inactive: " + sku);
        }
        if (variant.stock() < line.quantity()) {
            throw new BusinessException("Insufficient stock for sku=" + sku
                    + ". available=" + variant.stock() + ", requested=" + line.quantity());
        }

        return new OrderCommand.Line(
                variant.productId().toString(),
                variant.sku(),
                variant.productName(),
                variant.primaryImage(),
                variant.size(),
                variant.color(),
                variant.price(),
                line.quantity());
    }

    private OrderItem toOrderItem(OrderCommand.Line line) {
        UUID productId;
        try {
            productId = UUID.fromString(requireText(line.productId(), "productId"));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("productId must be a valid UUID");
        }
        return OrderItem.create(
                productId,
                requireText(line.sku(), "sku"),
                requireText(line.name(), "name"),
                line.image(),
                line.size(),
                line.color(),
                line.unitPrice(),
                line.quantity());
    }

    private void validateCheckout(OrderCommand.Checkout cmd) {
        if (cmd == null) {
            throw new BusinessException("checkout request is required");
        }
        if (cmd.userId() == null) {
            throw new BusinessException("userId is required");
        }
        if (cmd.lines() == null || cmd.lines().isEmpty()) {
            throw new BusinessException("Order must have at least one item");
        }
    }

    private void validateCheckoutBySku(OrderCommand.CheckoutBySku cmd) {
        if (cmd == null) {
            throw new BusinessException("checkout request is required");
        }
        if (cmd.userId() == null) {
            throw new BusinessException("userId is required");
        }
        if (cmd.paymentMethod() == null) {
            throw new BusinessException("paymentMethod is required");
        }
        if (cmd.lines() == null || cmd.lines().isEmpty()) {
            throw new BusinessException("Order must have at least one item");
        }
    }

    private void validateShippingAddress(String shippingAddress) {
        requireText(shippingAddress, "shippingAddress");
        try {
            if (!objectMapper.readTree(shippingAddress).isObject()) {
                throw new BusinessException("shippingAddress must be a JSON object string");
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessException("shippingAddress must be valid JSON");
        }
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(requireText(raw, "paymentMethod").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("paymentMethod must be COD or VNPAY");
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " is required");
        }
        return value.trim();
    }
}
