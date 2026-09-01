package com.capitec.capibook.branch.dto;

import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        String branchCode,
        String name,
        String address,
        String city,
        String province,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String phoneNumber,
        String email,
        boolean active,
        List<OperatingHoursEntry> operatingHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BranchResponse from(Branch branch, List<BranchOperatingHours> hours) {
        return new BranchResponse(
                branch.getId(),
                branch.getBranchCode(),
                branch.getName(),
                branch.getAddress(),
                branch.getCity(),
                branch.getProvince(),
                branch.getPostalCode(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getPhoneNumber(),
                branch.getEmail(),
                branch.isActive(),
                hours.stream().map(OperatingHoursEntry::from).toList(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}
