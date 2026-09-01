package com.capitec.capibook.servicecatalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateServiceRequest(
        @NotBlank String name,
        String description,
        @Min(1) int durationMinutes
) {}
