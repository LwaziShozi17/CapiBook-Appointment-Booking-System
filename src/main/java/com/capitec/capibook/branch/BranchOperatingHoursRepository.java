package com.capitec.capibook.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchOperatingHoursRepository extends JpaRepository<BranchOperatingHours, UUID> {

    List<BranchOperatingHours> findByBranchId(UUID branchId);

    Optional<BranchOperatingHours> findByBranchIdAndDayOfWeek(UUID branchId, DayOfWeek dayOfWeek);

    void deleteByBranchId(UUID branchId);
}
