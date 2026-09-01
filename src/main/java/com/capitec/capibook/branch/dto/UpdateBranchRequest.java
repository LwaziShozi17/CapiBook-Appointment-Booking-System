package com.capitec.capibook.branch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateBranchRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String province,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "Postal code must be 4 digits") String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String phoneNumber,
        @Email String email,
        @Min(1) Integer maxConcurrentAppointments
) {}
