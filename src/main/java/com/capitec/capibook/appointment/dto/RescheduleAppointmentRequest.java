package com.capitec.capibook.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RescheduleAppointmentRequest(
        @NotNull(message = "Branch ID is required") UUID branchId,
        @NotNull(message = "Service ID is required") UUID serviceId,
        @NotNull(message = "Appointment date is required") @Future(message = "Appointment date must be in the future") LocalDate appointmentDate,
        @NotNull(message = "Start time is required") LocalTime startTime,
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {}
