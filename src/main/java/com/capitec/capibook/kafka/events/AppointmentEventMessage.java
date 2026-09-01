package com.capitec.capibook.kafka.events;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentEventMessage(
        UUID eventId,
        AppointmentEventType eventType,
        Instant occurredAt,
        UUID appointmentId,
        UUID customerId,
        UUID branchId,
        UUID serviceId,
        LocalDate appointmentDate,
        LocalTime startTime,
        String referenceNumber
) {}
