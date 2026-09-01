package com.capitec.capibook.audit;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditConsumer(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                "appointment.created",
                "appointment.cancelled",
                "appointment.confirmed",
                "appointment.completed",
                "appointment.no_show",
                "appointment.rescheduled"
            },
            groupId = "capibook-audit-group"
    )
    public void handle(String payload) {
        try {
            AppointmentEventMessage message = objectMapper.readValue(payload, AppointmentEventMessage.class);
            AuditLog entry = new AuditLog();
            entry.setActorId(message.customerId());
            entry.setAction(message.eventType().name());
            entry.setEntityType("Appointment");
            entry.setEntityId(message.appointmentId().toString());
            entry.setDetails(payload);
            auditLogRepository.save(entry);
            log.debug("Audit log written for {} appointment {}", message.eventType(), message.appointmentId());
        } catch (Exception e) {
            log.error("AuditConsumer failed to process message: {}", e.getMessage());
        }
    }
}
