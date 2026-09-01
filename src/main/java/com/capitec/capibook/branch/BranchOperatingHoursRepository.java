package com.capitec.capibook.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchOperatingHoursRepository extends JpaRepository<BranchOperatingHours, UUID> {

    List<BranchOperatingHours> findByBranchId(UUID branchId);

    void deleteByBranchId(UUID branchId);
}
