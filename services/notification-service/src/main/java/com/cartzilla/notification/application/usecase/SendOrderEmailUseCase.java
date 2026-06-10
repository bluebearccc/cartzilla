package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.EmailLog;
import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.EmailLogRepository;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.domain.vo.NotificationPriority;
import com.cartzilla.notification.domain.vo.NotificationType;
import com.cartzilla.notification.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendOrderEmailUseCase {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    @Transactional
    public void sendConfirmed(UUID orderId, String email) {
        String to = email != null ? email : "customer@cartzilla.local";
        String subject = "Đơn hàng " + orderId + " đã được xác nhận";
        String body = "Cảm ơn bạn đã mua hàng tại Cartzilla! Đơn của bạn đang được xử lý.";

        Notification notification = notificationRepository.save(Notification.create(
                null, orderId, NotificationType.ORDER_CONFIRMED,
                "Đơn hàng đã xác nhận", body, NotificationPriority.NORMAL, null));

        EmailLog emailLog = emailLogRepository.save(EmailLog.create(
                notification.getId(), orderId, to, subject, "order_confirmed", "SMTP"));

        try {
            emailService.send(to, subject, body);
            emailLog.markSent(null);
        } catch (Exception e) {
            log.error("Failed to send ORDER_CONFIRMED email for order {}: {}", orderId, e.getMessage());
            emailLog.markFailed(e.getMessage());
        }
        emailLogRepository.save(emailLog);
    }

    @Transactional
    public void sendCancelled(UUID orderId, String reason, String email) {
        String to = email != null ? email : "customer@cartzilla.local";
        String body = "Rất tiếc, đơn hàng của bạn đã bị hủy. Lý do: " + reason;
        String subject = "Đơn hàng " + orderId + " đã bị hủy";

        Notification notification = notificationRepository.save(Notification.create(
                null, orderId, NotificationType.ORDER_CANCELLED,
                "Đơn hàng đã hủy", body, NotificationPriority.HIGH, null));

        EmailLog emailLog = emailLogRepository.save(EmailLog.create(
                notification.getId(), orderId, to, subject, "order_cancelled", "SMTP"));

        try {
            emailService.send(to, subject, body);
            emailLog.markSent(null);
        } catch (Exception e) {
            log.error("Failed to send ORDER_CANCELLED email for order {}: {}", orderId, e.getMessage());
            emailLog.markFailed(e.getMessage());
        }
        emailLogRepository.save(emailLog);
    }
}
