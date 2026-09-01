package com.capitec.capibook.availability.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(
        UUID branchId,
        UUID serviceId,
        LocalDate date,
        List<SlotResponse> slots
) {}
