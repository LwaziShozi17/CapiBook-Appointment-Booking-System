package com.capitec.capibook.notification;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.kafka.events.AppointmentEventType;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Phase 7 — NotificationConsumer + NotificationService pipeline.
 *
 * Strategy: call NotificationConsumer.handle() directly (same approach as KafkaEventIntegrationTest)
 * to verify the full consumer → service → repository chain against the H2 test database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DirtiesContext
class NotificationIntegrationTest {

    @Autowired
    private NotificationConsumer notificationConsumer;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        userRepository.findByEmail("notif-test@capibook.dev").ifPresentOrElse(
            existing -> customerId = existing.getId(),
            () -> {
                User user = new User();
                user.setEmail("notif-test@capibook.dev");
                user.setFirstName("Thabo");
                user.setLastName("Mokoena");
                user.setPasswordHash("$2a$10$hashedpassword");
                user.setRole(Role.CUSTOMER);
                customerId = userRepository.save(user).getId();
            }
        );
    }

    @Test
    void handle_appointmentCreatedEvent_savesNotificationWithStatusSent() throws Exception {
        String payload = objectMapper.writeValueAsString(
                buildMessage(AppointmentEventType.APPOINTMENT_CREATED));

        notificationConsumer.handle(payload);

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        Notification n = notifications.get(0);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(n.getUserId()).isEqualTo(customerId);
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test
    void handle_allNotificationEventTypes_eachSavesNotificationRecord() throws Exception {
        AppointmentEventType[] triggers = {
            AppointmentEventType.APPOINTMENT_CREATED,
            AppointmentEventType.APPOINTMENT_CONFIRMED,
            AppointmentEventType.APPOINTMENT_CANCELLED,
            AppointmentEventType.APPOINTMENT_RESCHEDULED
        };
        for (AppointmentEventType type : triggers) {
            notificationConsumer.handle(objectMapper.writeValueAsString(buildMessage(type)));
        }

        assertThat(notificationRepository.count()).isEqualTo(4);
    }

    @Test
    void handle_customerNotFound_doesNotSaveNotification() throws Exception {
        AppointmentEventMessage message = new AppointmentEventMessage(
                UUID.randomUUID(),
                AppointmentEventType.APPOINTMENT_CREATED,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(3),
                LocalTime.of(9, 0),
                "CAP-TEST-NOTFOUND"
        );

        notificationConsumer.handle(objectMapper.writeValueAsString(message));

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void handle_invalidJson_doesNotThrowAndSavesNoNotification() {
        notificationConsumer.handle("{ this is not valid json }");

        assertThat(notificationRepository.count()).isZero();
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
                LocalTime.of(10, 30),
                "CAP-TEST-NOTIF1"
        );
    }
}
