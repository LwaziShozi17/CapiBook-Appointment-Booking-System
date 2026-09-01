package com.capitec.capibook.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankingServiceRepository extends JpaRepository<BankingService, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    List<BankingService> findAllByActiveTrue();

    Optional<BankingService> findByIdAndActiveTrue(UUID id);
}
