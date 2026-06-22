package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.domain.vo.NotificationPriority;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import com.cartzilla.notification.domain.vo.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** F12 — BR-N04: customer đọc notification của mình → READ; không phải của mình → 404. */
class MarkNotificationReadUseCaseTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final MarkNotificationReadUseCase useCase = new MarkNotificationReadUseCase(repository);

    @Test
    void marksRead_whenOwnedByUser() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification n = Notification.create(userId, UUID.randomUUID(),
                NotificationType.ORDER_CONFIRMED, "Tiêu đề", "Nội dung",
                NotificationPriority.NORMAL, null);
        when(repository.findByIdAndRecipientUserId(id, userId)).thenReturn(Optional.of(n));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = useCase.execute(id, userId);

        assertEquals(NotificationStatus.READ, result.getStatus());
        assertNotNull(result.getReadAt());
        verify(repository).save(n);
    }

    @Test
    void throwsNotFound_whenNotOwnedByUser() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndRecipientUserId(id, userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> useCase.execute(id, userId));
        verify(repository, never()).save(any());
    }
}
