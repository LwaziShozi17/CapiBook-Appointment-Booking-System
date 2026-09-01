package com.capitec.capibook.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByBranchCode(String branchCode);

    List<Branch> findAllByActiveTrue();

    Optional<Branch> findByIdAndActiveTrue(UUID id);
}
