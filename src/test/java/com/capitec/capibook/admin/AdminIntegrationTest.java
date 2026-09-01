package com.capitec.capibook.admin;

import com.capitec.capibook.admin.dto.*;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.availability.AvailabilityService;
import com.capitec.capibook.availability.dto.AvailabilityResponse;
import com.capitec.capibook.branch.ExceptionType;
import com.capitec.capibook.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SYSTEM_ADMIN_EMAIL = "admin_integ_sysadmin@test.com";
    private static final String BRANCH_ADMIN_EMAIL = "admin_integ_branchadmin@test.com";
    private static final String CUSTOMER_EMAIL = "admin_integ_customer@test.com";
    private static final String OTHER_BRANCH_ADMIN_EMAIL = "admin_integ_other_branchadmin@test.com";
    private static final String PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final String BRANCH_ID = "dddddddd-0000-4000-8000-000000000001";
    private static final String OTHER_BRANCH_ID = "dddddddd-0000-4000-8000-000000000002";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM branch_availability_exceptions");
        jdbcTemplate.execute("DELETE FROM appointment_history");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?, ?, ?)",
                SYSTEM_ADMIN_EMAIL, BRANCH_ADMIN_EMAIL, CUSTOMER_EMAIL, OTHER_BRANCH_ADMIN_EMAIL);
        jdbcTemplate.execute("DELETE FROM branch_operating_hours WHERE branch_id IN " +
                "('" + BRANCH_ID + "','" + OTHER_BRANCH_ID + "')");
        jdbcTemplate.execute("DELETE FROM branches WHERE id IN " +
                "('" + BRANCH_ID + "','" + OTHER_BRANCH_ID + "')");

        // System admin
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'System', 'Admin', 'SYSTEM_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SYSTEM_ADMIN_EMAIL, PW_HASH);

        // Customer
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Integ', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, PW_HASH);

        // Branches
        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'ADM001', 'Admin Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID);
        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'ADM002', 'Other Branch', '2 Test St', 'Cape Town', 'Western Cape', '8001', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                OTHER_BRANCH_ID);

        // Branch admin for the primary branch
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, branch_id, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Branch', 'Admin', 'BRANCH_ADMIN', ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ADMIN_EMAIL, PW_HASH, UUID.fromString(BRANCH_ID));

        // Branch admin for the other branch
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, branch_id, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Other', 'Admin', 'BRANCH_ADMIN', ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                OTHER_BRANCH_ADMIN_EMAIL, PW_HASH, UUID.fromString(OTHER_BRANCH_ID));

        // Operating hours
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            jdbcTemplate.update(
                    "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                    "VALUES (RANDOM_UUID(), ?, ?, '08:00', '17:00', false)",
                    BRANCH_ID, day.name());
            jdbcTemplate.update(
                    "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                    "VALUES (RANDOM_UUID(), ?, ?, '08:00', '17:00', false)",
                    OTHER_BRANCH_ID, day.name());
        }
    }

    @Test
    void systemAdmin_canCreateBranchAdmin() {
        CreateBranchAdminRequest request = new CreateBranchAdminRequest(
                "new_branch_admin@test.com",
                "New",
                "Admin",
                null,
                UUID.fromString(BRANCH_ID),
                "securePassword123"
        );

        AdminUserResponse response = adminService.createBranchAdmin(request);

        assertThat(response.role()).isEqualTo("BRANCH_ADMIN");
        assertThat(response.branchId()).isEqualTo(UUID.fromString(BRANCH_ID));
        assertThat(response.email()).isEqualTo("new_branch_admin@test.com");
        assertThat(response.active()).isTrue();

        // Clean up
        jdbcTemplate.update("DELETE FROM users WHERE email = 'new_branch_admin@test.com'");
    }

    @Test
    void branchAdmin_seesOnlyTheirBranchAppointments() {
        // Book an appointment at the primary branch
        LocalDate futureDate = nextFutureWeekday();
        jdbcTemplate.update(
                "INSERT INTO appointments (id, customer_id, branch_id, service_id, appointment_date, start_time, end_time, " +
                "status, reference_number, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), " +
                "(SELECT id FROM users WHERE email = ?), ?, ?, ?, '09:00', '09:15', 'PENDING', 'CAP-TEST-A1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, BRANCH_ID, SERVICE_ID, futureDate);

        // BRANCH_ADMIN for primary branch sees 1 appointment
        PageResponse<AppointmentResponse> result = adminService.listAppointmentsForAdmin(
                BRANCH_ADMIN_EMAIL, null, PageRequest.of(0, 20));
        assertThat(result.totalElements()).isEqualTo(1);

        // BRANCH_ADMIN for other branch sees 0 appointments
        PageResponse<AppointmentResponse> otherResult = adminService.listAppointmentsForAdmin(
                OTHER_BRANCH_ADMIN_EMAIL, null, PageRequest.of(0, 20));
        assertThat(otherResult.totalElements()).isEqualTo(0);
    }

    @Test
    void systemAdmin_seesAllAppointments() {
        LocalDate futureDate = nextFutureWeekday();

        jdbcTemplate.update(
                "INSERT INTO appointments (id, customer_id, branch_id, service_id, appointment_date, start_time, end_time, " +
                "status, reference_number, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), " +
                "(SELECT id FROM users WHERE email = ?), ?, ?, ?, '09:00', '09:15', 'PENDING', 'CAP-TEST-B1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, BRANCH_ID, SERVICE_ID, futureDate);

        jdbcTemplate.update(
                "INSERT INTO appointments (id, customer_id, branch_id, service_id, appointment_date, start_time, end_time, " +
                "status, reference_number, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), " +
                "(SELECT id FROM users WHERE email = ?), ?, ?, ?, '10:00', '10:15', 'PENDING', 'CAP-TEST-B2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, OTHER_BRANCH_ID, SERVICE_ID, futureDate);

        PageResponse<AppointmentResponse> result = adminService.listAppointmentsForAdmin(
                SYSTEM_ADMIN_EMAIL, null, PageRequest.of(0, 20));
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void systemAdmin_canDeactivateCustomer() {
        String userIdStr = jdbcTemplate.queryForObject(
                "SELECT CAST(id AS VARCHAR) FROM users WHERE email = ?", String.class, CUSTOMER_EMAIL);
        UUID userId = UUID.fromString(userIdStr);

        AdminUserResponse response = adminService.deactivateUser(userId);

        assertThat(response.active()).isFalse();
        assertThat(response.email()).isEqualTo(CUSTOMER_EMAIL);
    }

    @Test
    void branchAvailabilityException_blocksSlotGeneration() {
        LocalDate futureDate = nextFutureWeekday();

        // Before exception: availability should return slots
        AvailabilityResponse before = availabilityService.getAvailability(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), futureDate);
        assertThat(before.slots()).isNotEmpty();

        // Create exception for that date
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(
                futureDate, ExceptionType.CLOSED, "Branch maintenance");
        adminService.createException(UUID.fromString(BRANCH_ID), request);

        // After exception: availability should return no slots
        AvailabilityResponse after = availabilityService.getAvailability(
                UUID.fromString(BRANCH_ID), UUID.fromString(SERVICE_ID), futureDate);
        assertThat(after.slots()).isEmpty();
    }

    private LocalDate nextFutureWeekday() {
        LocalDate date = LocalDate.now().plusDays(10);
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
