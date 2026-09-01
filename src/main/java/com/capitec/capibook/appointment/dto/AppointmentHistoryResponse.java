package com.capitec.capibook.appointment.dto;

import com.capitec.capibook.appointment.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentHistoryResponse(
        UUID id,
        UUID appointmentId,
        AppointmentStatus previousStatus,
        AppointmentStatus newStatus,
        UUID changedById,
        String changedByFirstName,
        String changedByLastName,
        String changeReason,
        LocalDateTime changedAt
) {}
