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

    @Query("SELECT a FROM Appointment a WHERE a.branch.id = :branchId")
    Page<Appointment> findByBranchId(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.branch.id = :branchId " +
           "AND a.appointmentDate BETWEEN :from AND :to " +
           "AND a.status IN :statuses")
    long countByBranchAndDateRangeAndStatuses(
            @Param("branchId") UUID branchId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<AppointmentStatus> statuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.appointmentDate BETWEEN :from AND :to " +
           "AND a.status = :status")
    long countByDateRangeAndStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") AppointmentStatus status);

    @Query("SELECT a.service.id, a.service.name, COUNT(a) FROM Appointment a " +
           "WHERE a.appointmentDate BETWEEN :from AND :to " +
           "GROUP BY a.service.id, a.service.name " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> findServicePopularity(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
