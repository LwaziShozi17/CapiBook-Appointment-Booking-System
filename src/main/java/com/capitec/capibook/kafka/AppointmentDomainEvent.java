package com.capitec.capibook.kafka;

import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent published within an active transaction.
 * The {@link AppointmentEventPublisher} listens via @TransactionalEventListener(AFTER_COMMIT)
 * so Kafka publication only happens after the database transaction commits successfully.
 */
public class AppointmentDomainEvent extends ApplicationEvent {

    private final AppointmentEventMessage message;

    public AppointmentDomainEvent(Object source, AppointmentEventMessage message) {
        super(source);
        this.message = message;
    }

    public AppointmentEventMessage getMessage() {
        return message;
    }
}
