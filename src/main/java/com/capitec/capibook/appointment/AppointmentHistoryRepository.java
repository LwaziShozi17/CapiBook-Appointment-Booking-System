package com.capitec.capibook.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {

    List<AppointmentHistory> findByAppointmentIdOrderByChangedAtAsc(UUID appointmentId);
}
