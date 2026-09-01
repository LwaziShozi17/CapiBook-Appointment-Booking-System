package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.common.PageResponse;
import com.capitec.capibook.exception.AppointmentConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AppointmentIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "dddddddd-0000-4000-8000-000000000001";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001"; // Card Collection 15 min
    private static final String CUSTOMER_EMAIL = "integration_customer@test.com";
    private static final String CUSTOMER2_EMAIL = "integration_customer2@test.com";
    private static final String PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("UPDATE users SET branch_id = NULL WHERE branch_id IS NOT NULL");
        jdbcTemplate.execute("DELETE FROM branch_availability_exceptions");
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches");
        jdbcTemplate.execute("DELETE FROM users WHERE email IN ('" + CUSTOMER_EMAIL + "', '" + CUSTOMER2_EMAIL + "')");

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Integration', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, PW_HASH
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Integration', 'Customer2', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER2_EMAIL, PW_HASH
        );

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'INT001', 'Integration Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID
        );

        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            jdbcTemplate.update(
                    "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                    "VALUES (RANDOM_UUID(), ?, ?, '08:00', '17:00', false)",
                    BRANCH_ID, day.name()
            );
        }
    }

    @Test
    void createAppointment_fullFlow_returnsConfirmedAppointment() {
        LocalDate date = nextFutureWeekday();
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, LocalTime.of(9, 0), "Test notes");

        AppointmentResponse response = appointmentService.createAppointment(CUSTOMER_EMAIL, request);

        assertThat(response.referenceNumber()).startsWith("CAP-");
        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.appointmentDate()).isEqualTo(date);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(9, 15)); // 15 min service
        assertThat(response.branchName()).isEqualTo("Integration Branch");
    }

    @Test
    void createAppointment_onPublicHoliday_throws() {
        // 2026-12-25 is Christmas — seeded in V10
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID),
                LocalDate.of(2026, 12, 25), LocalTime.of(9, 0), null);

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public holiday");
    }

    @Test
    void createAppointment_onWeekend_throws() {
        // Find a future Saturday
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SATURDAY) {
            date = date.plusDays(1);
        }
        final LocalDate saturday = date;
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), saturday, LocalTime.of(9, 0), null);

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void createAppointment_duplicateByCustomer_throwsConflict() {
        LocalDate date = nextFutureWeekday();
        LocalTime slotTime = LocalTime.of(10, 0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, slotTime, null);

        appointmentService.createAppointment(CUSTOMER_EMAIL, request);

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("already have an appointment");
    }

    @Test
    void createAppointment_slotFullyBooked_throwsConflict() {
        LocalDate date = nextFutureWeekday();
        LocalTime slotTime = LocalTime.of(11, 0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, slotTime, null);

        // Customer 1 books the slot (capacity = 1)
        appointmentService.createAppointment(CUSTOMER_EMAIL, request);

        // Customer 2 tries the same slot
        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER2_EMAIL, request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void getMyAppointments_returnsOnlyCustomerAppointments() {
        LocalDate date = nextFutureWeekday();
        CreateAppointmentRequest req1 = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, LocalTime.of(8, 0), null);
        CreateAppointmentRequest req2 = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, LocalTime.of(8, 15), null);

        appointmentService.createAppointment(CUSTOMER_EMAIL, req1);
        appointmentService.createAppointment(CUSTOMER_EMAIL, req2);
        // Customer2 also books a different slot — should not appear in customer1's list
        CreateAppointmentRequest req3 = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, LocalTime.of(8, 30), null);
        appointmentService.createAppointment(CUSTOMER2_EMAIL, req3);

        PageResponse<AppointmentResponse> page = appointmentService.getMyAppointments(
                CUSTOMER_EMAIL, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "appointmentDate")));

        assertThat(page.content()).hasSize(2);
        assertThat(page.content()).allMatch(a -> a.customerFirstName().equals("Integration"));
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void createAppointment_concurrentRequests_onlyOneSucceeds() throws InterruptedException {
        LocalDate date = nextFutureWeekday();
        LocalTime slotTime = LocalTime.of(14, 0);

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), date, slotTime, null);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        String[] emails = {CUSTOMER_EMAIL, CUSTOMER2_EMAIL};
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (String email : emails) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    appointmentService.createAppointment(email, request);
                    successCount.incrementAndGet();
                } catch (AppointmentConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    private LocalDate nextFutureWeekday() {
        LocalDate date = LocalDate.now().plusDays(8); // 8 days to avoid same-day edge cases
        java.util.Set<LocalDate> holidays = java.util.Set.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 16), LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 9, 24), LocalDate.of(2026, 12, 16),
                LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26)
        );
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || holidays.contains(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
