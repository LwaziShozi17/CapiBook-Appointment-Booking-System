package com.capitec.capibook.notification;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Notification consumer skeleton — Phase 7 will implement the full notification pipeline.
 * Currently logs the received event; the NotificationService and provider abstraction
 * (email / mock) will be wired in Phase 7.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;

    public NotificationConsumer(ObjectMapper objectMapper) {
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
            log.info("[NOTIFICATION] {} — appointment {} ({})",
                    message.eventType(), message.referenceNumber(), message.appointmentId());
        } catch (Exception e) {
            log.error("NotificationConsumer failed to process message: {}", e.getMessage());
        }
    }
}
