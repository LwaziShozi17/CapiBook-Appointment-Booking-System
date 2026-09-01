package com.capitec.capibook.notification;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.kafka.events.AppointmentEventType;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationProvider notificationProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID customerId;
    private User customer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new User();
        customer.setEmail("customer@test.com");
        customer.setFirstName("Lwazi");
        customer.setLastName("Test");
    }

    @Test
    void notify_appointmentCreated_callsProviderWithConfirmationSubject() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProvider).send(eq("customer@test.com"), subjectCaptor.capture(), any());
        assertThat(subjectCaptor.getValue()).contains("Booking Confirmation");
        assertThat(subjectCaptor.getValue()).contains("CAP-TEST-00001");
    }

    @Test
    void notify_appointmentConfirmed_callsProviderWithConfirmedSubject() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CONFIRMED));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProvider).send(eq("customer@test.com"), subjectCaptor.capture(), any());
        assertThat(subjectCaptor.getValue()).contains("Appointment Confirmed");
    }

    @Test
    void notify_appointmentCancelled_callsProviderWithCancellationSubject() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CANCELLED));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProvider).send(eq("customer@test.com"), subjectCaptor.capture(), any());
        assertThat(subjectCaptor.getValue()).contains("Appointment Cancelled");
    }

    @Test
    void notify_appointmentRescheduled_callsProviderWithRescheduledSubject() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_RESCHEDULED));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProvider).send(eq("customer@test.com"), subjectCaptor.capture(), any());
        assertThat(subjectCaptor.getValue()).contains("Appointment Rescheduled");
    }

    @Test
    void notify_onSuccess_savesNotificationWithStatusSent() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getUserId()).isEqualTo(customerId);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    void notify_providerThrowsException_savesNotificationWithStatusFailed() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(notificationProvider).send(any(), any(), any());

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.getErrorMessage()).contains("SMTP connection refused");
        assertThat(saved.getSentAt()).isNull();
    }

    @Test
    void notify_userNotFound_skipsProviderAndDoesNotSaveNotification() {
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        verifyNoInteractions(notificationProvider);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void notify_bodyContainsCustomerFirstName() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        notificationService.notify(buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationProvider).send(any(), any(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("Lwazi");
    }

    private AppointmentEventMessage buildMessage(AppointmentEventType type) {
        return new AppointmentEventMessage(
                UUID.randomUUID(),
                type,
                Instant.now(),
                UUID.randomUUID(),
                customerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(7),
                LocalTime.of(10, 0),
                "CAP-TEST-00001"
        );
    }
}
