package com.capitec.capibook.appointment.dto;

import com.capitec.capibook.appointment.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String referenceNumber,
        UUID customerId,
        String customerFirstName,
        String customerLastName,
        UUID branchId,
        String branchName,
        UUID serviceId,
        String serviceName,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
