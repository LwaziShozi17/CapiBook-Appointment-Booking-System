package com.capitec.capibook.admin;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Analytics consumer skeleton — Phase 8 will implement aggregation and dashboard data.
 * Currently logs the received event; analytics persistence will be wired in Phase 8.
 */
@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    private final ObjectMapper objectMapper;

    public AnalyticsConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                "appointment.created",
                "appointment.completed",
                "appointment.no_show"
            },
            groupId = "capibook-analytics-group"
    )
    public void handle(String payload) {
        try {
            AppointmentEventMessage message = objectMapper.readValue(payload, AppointmentEventMessage.class);
            log.debug("[ANALYTICS] {} — appointment {}", message.eventType(), message.appointmentId());
        } catch (Exception e) {
            log.error("AnalyticsConsumer failed to process message: {}", e.getMessage());
        }
    }
}
