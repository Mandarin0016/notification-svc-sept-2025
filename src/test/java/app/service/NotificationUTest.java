package app.service;

import app.exception.NotificationPreferenceDisabledException;
import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationUTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PreferenceService preferenceService;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void retryFailed_whenPreferenceIsTurnedOff_thenThrowsException() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(false)
                .build();
        when(preferenceService.getByUserId(userId)).thenReturn(notificationPreference);

        // When & Then
        assertThrows(NotificationPreferenceDisabledException.class, () -> notificationService.retryFailed(userId));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verifyNoInteractions(mailSender);
    }
    @Test
    void retryFailed_whenPreferenceIsTurnedOnAndThereAre2FailedEmails_thenRetryExactly2Times() {

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference notificationPreference = NotificationPreference.builder()
                .enabled(true)
                .build();
        when(preferenceService.getByUserId(userId)).thenReturn(notificationPreference);

        List<Notification> failedEmails = new ArrayList<>();
        Notification failedNotification1 = Notification.builder()
                .deleted(false)
                .status(NotificationStatus.FAILED)
                .build();
        Notification failedNotification2 = Notification.builder()
                .deleted(false)
                .status(NotificationStatus.FAILED)
                .build();
        Notification failedNotification3 = Notification.builder()
                .deleted(true)
                .status(NotificationStatus.FAILED)
                .build();
        failedEmails.add(failedNotification1);
        failedEmails.add(failedNotification2);
        failedEmails.add(failedNotification3);
        when(notificationRepository.findByUserId(userId)).thenReturn(failedEmails);

        // When
        notificationService.retryFailed(userId);

        // Then
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        assertEquals(NotificationStatus.SUCCEEDED, failedNotification1.getStatus());
        assertEquals(NotificationStatus.SUCCEEDED, failedNotification2.getStatus());
        assertEquals(NotificationStatus.FAILED, failedNotification3.getStatus());
    }
}
