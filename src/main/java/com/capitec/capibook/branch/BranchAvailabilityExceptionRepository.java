package com.capitec.capibook.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BranchAvailabilityExceptionRepository extends JpaRepository<BranchAvailabilityException, UUID> {

    List<BranchAvailabilityException> findByBranchIdOrderByExceptionDateAsc(UUID branchId);

    boolean existsByBranchIdAndExceptionDate(UUID branchId, LocalDate date);
}
