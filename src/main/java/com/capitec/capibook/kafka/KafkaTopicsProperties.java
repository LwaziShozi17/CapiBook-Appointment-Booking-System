package com.capitec.capibook.kafka;

import com.capitec.capibook.kafka.events.AppointmentEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String appointmentCreated,
        String appointmentCancelled,
        String appointmentConfirmed,
        String appointmentCompleted,
        String appointmentNoShow,
        String appointmentRescheduled
) {
    public String topicFor(AppointmentEventType eventType) {
        return switch (eventType) {
            case APPOINTMENT_CREATED    -> appointmentCreated;
            case APPOINTMENT_CANCELLED  -> appointmentCancelled;
            case APPOINTMENT_CONFIRMED  -> appointmentConfirmed;
            case APPOINTMENT_COMPLETED  -> appointmentCompleted;
            case APPOINTMENT_NO_SHOW    -> appointmentNoShow;
            case APPOINTMENT_RESCHEDULED -> appointmentRescheduled;
        };
    }
}
