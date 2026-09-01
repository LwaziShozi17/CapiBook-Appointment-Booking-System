package com.capitec.capibook.branch.dto;

import com.capitec.capibook.branch.BranchOperatingHours;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperatingHoursEntry(
        @NotNull DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
    public static OperatingHoursEntry from(BranchOperatingHours entity) {
        return new OperatingHoursEntry(
                entity.getDayOfWeek(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.isClosed()
        );
    }
}
