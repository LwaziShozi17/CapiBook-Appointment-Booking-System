package com.capitec.capibook.admin.dto;

import java.time.LocalDate;
import java.util.UUID;

public record BranchUtilisationResponse(
        UUID branchId,
        String branchName,
        LocalDate periodStart,
        LocalDate periodEnd,
        long totalSlots,
        long bookedSlots,
        double utilisation
) {}
