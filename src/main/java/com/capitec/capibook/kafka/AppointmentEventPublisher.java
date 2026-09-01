package com.capitec.capibook.kafka;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Component
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topicsProperties;

    public AppointmentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     KafkaTopicsProperties topicsProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicsProperties = topicsProperties;
    }

    /**
     * Fires after the database transaction commits. Kafka failure is caught and logged
     * so it never propagates back to the HTTP caller — the booking is already persisted.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentEvent(AppointmentDomainEvent event) {
        AppointmentEventMessage message = event.getMessage();
        try {
            String topic = topicsProperties.topicFor(message.eventType());
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, message.appointmentId().toString(), payload);
            log.debug("Published {} to topic [{}] for appointment {}",
                    message.eventType(), topic, message.appointmentId());
        } catch (Exception e) {
            log.error("Failed to publish Kafka event {} for appointment {}: {}",
                    message.eventType(), message.appointmentId(), e.getMessage());
        }
    }
}
