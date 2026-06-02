package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.NotificationLog;
import com.cartzilla.notification.domain.repository.NotificationLogRepository;
import com.cartzilla.notification.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendOrderEmailUseCase {

    private final EmailService emailService;
    private final NotificationLogRepository logRepository;

    public void sendConfirmed(UUID orderId, String email) {
        String to = email != null ? email : "customer@cartzilla.local";
        NotificationLog log = logRepository.save(
                NotificationLog.of(orderId, to, "ORDER_CONFIRMED", "Đơn hàng đã xác nhận"));
        try {
            emailService.send(to, "Đơn hàng " + orderId + " đã được xác nhận",
                    "Cảm ơn bạn đã mua hàng tại Cartzilla! Đơn của bạn đang được xử lý.");
            log.markSent();
        } catch (Exception e) {
            log.markFailed(e.getMessage());
        }
        logRepository.save(log);
    }

    public void sendCancelled(UUID orderId, String reason, String email) {
        String to = email != null ? email : "customer@cartzilla.local";
        NotificationLog log = logRepository.save(
                NotificationLog.of(orderId, to, "ORDER_CANCELLED", "Đơn hàng đã hủy"));
        try {
            emailService.send(to, "Đơn hàng " + orderId + " đã bị hủy",
                    "Rất tiếc, đơn hàng của bạn đã bị hủy. Lý do: " + reason);
            log.markSent();
        } catch (Exception e) {
            log.markFailed(e.getMessage());
        }
        logRepository.save(log);
    }
}
