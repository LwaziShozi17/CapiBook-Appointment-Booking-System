package com.capitec.capibook.notification;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                "appointment.created",
                "appointment.cancelled",
                "appointment.confirmed",
                "appointment.rescheduled"
            },
            groupId = "capibook-notification-group"
    )
    public void handle(String payload) {
        try {
            AppointmentEventMessage message = objectMapper.readValue(payload, AppointmentEventMessage.class);
            log.debug("[NOTIFICATION] Processing {} for appointment {}",
                    message.eventType(), message.referenceNumber());
            notificationService.notify(message);
        } catch (Exception e) {
            log.error("NotificationConsumer failed to process message: {}", e.getMessage());
        }
    }
}
