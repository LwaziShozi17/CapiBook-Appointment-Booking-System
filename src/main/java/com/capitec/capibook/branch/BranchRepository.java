package com.capitec.capibook.branch;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByBranchCode(String branchCode);

    List<Branch> findAllByActiveTrue();

    Optional<Branch> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Branch b WHERE b.id = :id AND b.active = true")
    Optional<Branch> findByIdAndActiveTrueForUpdate(@Param("id") UUID id);
}
