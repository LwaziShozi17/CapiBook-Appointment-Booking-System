package com.capitec.capibook.admin.dto;

import java.time.LocalDate;

public record AppointmentSummaryResponse(
        long totalBooked,
        long totalPending,
        long totalConfirmed,
        long totalCancelled,
        long totalCompleted,
        long totalNoShow,
        long totalRescheduled,
        LocalDate periodStart,
        LocalDate periodEnd
) {}
