package com.capitec.capibook.availability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    boolean existsByDate(LocalDate date);
}
