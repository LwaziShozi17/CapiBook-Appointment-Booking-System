package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentHistoryResponse;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.appointment.dto.RescheduleAppointmentRequest;
import com.capitec.capibook.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AppointmentLifecycleIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "ffffffff-0000-4000-8000-000000000001";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001";
    private static final String CUSTOMER_EMAIL = "lifecycle_customer@test.com";
    private static final String ADMIN_EMAIL = "lifecycle_admin@test.com";
    private static final String PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM appointment_history");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches WHERE id = '" + BRANCH_ID + "'");
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)", CUSTOMER_EMAIL, ADMIN_EMAIL);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Lifecycle', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, PW_HASH);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Lifecycle', 'Admin', 'BRANCH_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ADMIN_EMAIL, PW_HASH);

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'LCI001', 'Lifecycle Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID);

        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            jdbcTemplate.update(
                    "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                    "VALUES (RANDOM_UUID(), ?, ?, '08:00', '17:00', false)",
                    BRANCH_ID, day.name());
        }
    }

    @Test
    void cancelPending_byCustomer_succeeds() {
        AppointmentResponse created = bookAppointment(LocalTime.of(9, 0));

        AppointmentResponse cancelled = appointmentService.cancelAppointment(
                created.id(), CUSTOMER_EMAIL, "Changed my mind");

        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void confirmThenComplete_fullFlow_succeeds() {
        AppointmentResponse created = bookAppointment(LocalTime.of(9, 0));

        AppointmentResponse confirmed = appointmentService.confirmAppointment(
                created.id(), ADMIN_EMAIL, null);
        assertThat(confirmed.status()).isEqualTo(AppointmentStatus.CONFIRMED);

        AppointmentResponse completed = appointmentService.completeAppointment(
                confirmed.id(), ADMIN_EMAIL, null);
        assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void confirmThenNoShow_succeeds() {
        AppointmentResponse created = bookAppointment(LocalTime.of(10, 0));

        appointmentService.confirmAppointment(created.id(), ADMIN_EMAIL, null);

        AppointmentResponse noShow = appointmentService.markNoShow(created.id(), ADMIN_EMAIL, "Did not arrive");
        assertThat(noShow.status()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void cancelTerminalAppointment_throws() {
        AppointmentResponse created = bookAppointment(LocalTime.of(11, 0));
        appointmentService.cancelAppointment(created.id(), CUSTOMER_EMAIL, null);

        assertThatThrownBy(() -> appointmentService.cancelAppointment(created.id(), CUSTOMER_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void reschedule_confirmedAppointment_createsNewAndMarksOriginalRescheduled() {
        AppointmentResponse created = bookAppointment(LocalTime.of(8, 0));
        appointmentService.confirmAppointment(created.id(), ADMIN_EMAIL, null);

        LocalDate newDate = nextFutureWeekday().plusDays(3);
        while (newDate.getDayOfWeek().getValue() > 5) newDate = newDate.plusDays(1);

        RescheduleAppointmentRequest rescheduleRequest = new RescheduleAppointmentRequest(
                UUID.fromString(BRANCH_ID),
                UUID.fromString(SERVICE_ID),
                newDate,
                LocalTime.of(14, 0),
                "Need a later date",
                null
        );

        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                created.id(), CUSTOMER_EMAIL, rescheduleRequest);

        assertThat(rescheduled.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(rescheduled.id()).isNotEqualTo(created.id());
        assertThat(rescheduled.startTime()).isEqualTo(LocalTime.of(14, 0));

        AppointmentResponse original = appointmentService.getAppointmentById(created.id(), ADMIN_EMAIL);
        assertThat(original.status()).isEqualTo(AppointmentStatus.RESCHEDULED);
    }

    @Test
    void history_recordsAllTransitions() {
        AppointmentResponse created = bookAppointment(LocalTime.of(9, 30));
        appointmentService.confirmAppointment(created.id(), ADMIN_EMAIL, "Looks good");
        appointmentService.cancelAppointment(created.id(), ADMIN_EMAIL, "Branch emergency");

        List<AppointmentHistoryResponse> history =
                appointmentService.getAppointmentHistory(created.id(), ADMIN_EMAIL);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).previousStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.get(0).newStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.get(0).changeReason()).isEqualTo("Looks good");
        assertThat(history.get(1).previousStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(history.get(1).newStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    private AppointmentResponse bookAppointment(LocalTime startTime) {
        LocalDate date = nextFutureWeekday();
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, startTime, null);
        return appointmentService.createAppointment(CUSTOMER_EMAIL, request);
    }

    private LocalDate nextFutureWeekday() {
        LocalDate date = LocalDate.now().plusDays(8);
        Set<LocalDate> holidays = Set.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 16), LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 9, 24), LocalDate.of(2026, 12, 16),
                LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26),
                LocalDate.of(2027, 1, 1)
        );
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || holidays.contains(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
