package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.EmailLog;
import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.EmailLogRepository;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.domain.vo.NotificationPriority;
import com.cartzilla.notification.domain.vo.NotificationType;
import com.cartzilla.notification.infrastructure.email.EmailService;
import com.cartzilla.notification.infrastructure.feign.UserFeignClient;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * F12 / UC-08: tạo in-app Notification (gắn recipientUserId) + EmailLog outbound
 * khi nhận order.confirmed / order.cancelled / order.shipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendOrderEmailUseCase {

    private static final String FALLBACK_EMAIL = "customer@cartzilla.local";

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserFeignClient userFeignClient;

    @Transactional
    public void sendConfirmed(UUID orderId, UUID recipientUserId) {
        String to = resolveEmail(recipientUserId);
        String subject = "Đơn hàng " + orderId + " đã được xác nhận";
        String body = "Cảm ơn bạn đã mua hàng tại Cartzilla! Đơn của bạn đang được xử lý.";
        deliver(recipientUserId, orderId, NotificationType.ORDER_CONFIRMED,
                "Đơn hàng đã xác nhận", body, NotificationPriority.NORMAL,
                to, subject, "order_confirmed");
    }

    @Transactional
    public void sendCancelled(UUID orderId, UUID recipientUserId, String reason) {
        String to = resolveEmail(recipientUserId);
        String body = "Rất tiếc, đơn hàng của bạn đã bị hủy. Lý do: " + reason;
        String subject = "Đơn hàng " + orderId + " đã bị hủy";
        deliver(recipientUserId, orderId, NotificationType.ORDER_CANCELLED,
                "Đơn hàng đã hủy", body, NotificationPriority.HIGH,
                to, subject, "order_cancelled");
    }

    @Transactional
    public void sendShipped(UUID orderId, UUID recipientUserId) {
        String to = resolveEmail(recipientUserId);
        String body = "Đơn hàng của bạn đang được giao. Vui lòng chú ý điện thoại.";
        String subject = "Đơn hàng " + orderId + " đang được giao";
        deliver(recipientUserId, orderId, NotificationType.ORDER_SHIPPED,
                "Đơn hàng đang giao", body, NotificationPriority.NORMAL,
                to, subject, "order_shipped");
    }

    /** Tạo Notification in-app + EmailLog, gửi email và cập nhật trạng thái email (BR-N01/BR-N05). */
    private void deliver(UUID recipientUserId, UUID orderId, NotificationType type,
                         String title, String message, NotificationPriority priority,
                         String to, String subject, String template) {
        Notification notification = notificationRepository.save(Notification.create(
                recipientUserId, orderId, type, title, message, priority, null));

        EmailLog emailLog = emailLogRepository.save(EmailLog.create(
                notification.getId(), orderId, to, subject, template, "SMTP"));

        try {
            emailService.send(to, subject, message);
            emailLog.markSent(null);
        } catch (Exception e) {
            log.error("Failed to send {} email for order {}: {}", type, orderId, e.getMessage());
            emailLog.markFailed(e.getMessage());
        }
        emailLogRepository.save(emailLog);
    }

    /** Tra email người nhận từ user-service; lỗi/null → fallback (NA-04). */
    private String resolveEmail(UUID recipientUserId) {
        if (recipientUserId == null) return FALLBACK_EMAIL;
        try {
            ApiResponse<UserFeignClient.UserContactDto> resp =
                    userFeignClient.getUserContact(recipientUserId);
            if (resp != null && resp.data() != null && resp.data().email() != null
                    && !resp.data().email().isBlank()) {
                return resp.data().email();
            }
        } catch (Exception ex) {
            log.warn("Cannot resolve email for user {}: {}", recipientUserId, ex.getMessage());
        }
        return FALLBACK_EMAIL;
    }
}
