package com.capitec.capibook.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.branch.id = :branchId " +
           "AND a.appointmentDate = :date " +
           "AND a.startTime = :startTime " +
           "AND a.status IN :statuses")
    long countBookedSlotsAt(
            @Param("branchId") UUID branchId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("statuses") List<AppointmentStatus> statuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.appointmentDate = :date " +
           "AND a.startTime = :startTime " +
           "AND a.status IN :statuses")
    long countActiveByCustomerAndDateTime(
            @Param("customerId") UUID customerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("statuses") List<AppointmentStatus> statuses);

    @Query("SELECT a FROM Appointment a WHERE a.customer.id = :customerId")
    Page<Appointment> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);
}
