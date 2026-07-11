package com.cartzilla.payment.application.usecase;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import com.cartzilla.payment.infrastructure.feign.OrderFeignClient;
import com.cartzilla.payment.infrastructure.vnpay.VnpayService;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * F13 — UC-07 VNPay: tạo (hoặc tái sử dụng) Payment VNPAY PENDING và sinh redirect URL đã ký.
 * BR-PY01: một payment/order — tái sử dụng nếu đã tồn tại.
 */
@Service
@RequiredArgsConstructor
public class CreateVnpayPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final VnpayService vnpayService;
    private final OrderFeignClient orderFeignClient;

    /**
     * @param clientAmount số tiền client gửi — CHỈ tham khảo/log, KHÔNG dùng. Số tiền thật
     *                     lấy từ order-service (server-authoritative) để chống thao túng.
     */
    @Transactional
    public Result execute(UUID orderId, UUID userId, BigDecimal clientAmount, String orderInfo, String ipAddr) {
        if (orderId == null) throw new BusinessException("orderId is required");

        // Luôn xác thực với order-service: ownership, phương thức, và đơn còn chờ thanh toán.
        // Chặn trường hợp khách mở lại URL cũ để trả tiền cho đơn đã hủy/đã xác nhận.
        OrderFeignClient.OrderPaymentInfo info = resolveOrderInfo(orderId, userId);
        if (!"VNPAY".equalsIgnoreCase(info.paymentMethod())) {
            // Phương thức thanh toán chốt tại lúc đặt hàng: đơn COD KHÔNG được trả VNPay.
            // Muốn đổi kiểu thanh toán thì phải hủy đơn và đặt lại.
            throw new BusinessException("Đơn hàng này đã chọn thanh toán " + info.paymentMethod()
                    + ". Muốn thanh toán bằng VNPay, vui lòng hủy đơn và đặt lại.");
        }
        if (info.status() != null && !"PENDING".equalsIgnoreCase(info.status())) {
            throw new BusinessException("Đơn hàng đang ở trạng thái " + info.status()
                    + ", không thể thanh toán VNPay nữa.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            // SECURITY: amount = tổng đơn thật từ order-service, bỏ qua clientAmount.
            payment = Payment.create(orderId, userId, PaymentMethod.VNPAY, info.amount());
            payment.attachVnpayRef(generateTxnRef());
            payment = paymentRepository.save(payment);
        } else {
            if (payment.getStatus() == PaymentStatus.PAID)
                throw new BusinessException("Đơn hàng đã được thanh toán");
            if (payment.getMethod() != PaymentMethod.VNPAY)
                throw new BusinessException("Đơn hàng này đã chọn thanh toán " + payment.getMethod()
                        + ". Muốn thanh toán bằng VNPay, vui lòng hủy đơn và đặt lại.");
            if (payment.getVnpayTxnRef() == null) {
                payment.attachVnpayRef(generateTxnRef());
                payment = paymentRepository.save(payment);
            }
        }

        String url = vnpayService.buildPaymentUrl(
                payment.getVnpayTxnRef(), payment.getAmount(), orderInfo, ipAddr);
        return new Result(url, payment.getVnpayTxnRef(), payment.getOrderId());
    }

    /** Lấy thông tin đơn thật từ order-service + verify đơn thuộc về user (fail-closed). */
    private OrderFeignClient.OrderPaymentInfo resolveOrderInfo(UUID orderId, UUID userId) {
        OrderFeignClient.OrderPaymentInfo data;
        try {
            var response = orderFeignClient.getPaymentInfo(orderId);
            data = response == null ? null : response.data();
            if (response == null || !response.success() || data == null || data.amount() == null) {
                throw new BusinessException("Không lấy được thông tin đơn hàng: " + orderId);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException("Không lấy được thông tin đơn hàng (order-service lỗi): " + orderId);
        }
        if (userId != null && data.userId() != null && !data.userId().equals(userId)) {
            throw new SecurityException("Đơn hàng không thuộc về bạn");
        }
        if (data.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền đơn hàng không hợp lệ: " + orderId);
        }
        return data;
    }

    private String generateTxnRef() {
        return "VNP" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    public record Result(String paymentUrl, String txnRef, UUID orderId) {}
}
