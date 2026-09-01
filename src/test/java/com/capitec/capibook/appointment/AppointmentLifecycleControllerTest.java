package com.capitec.capibook.appointment;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentLifecycleControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "eeeeeeee-0000-4000-8000-000000000001";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001";
    private static final String CUSTOMER_EMAIL = "lifecycle_ctrl_customer@test.com";
    private static final String OTHER_CUSTOMER_EMAIL = "lifecycle_ctrl_other@test.com";
    private static final String ADMIN_EMAIL = "lifecycle_ctrl_admin@test.com";
    private static final String PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        jdbcTemplate.execute("DELETE FROM appointment_history");
        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.update(
                "DELETE FROM users WHERE email IN (?, ?, ?)", CUSTOMER_EMAIL, OTHER_CUSTOMER_EMAIL, ADMIN_EMAIL);
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches WHERE id = '" + BRANCH_ID + "'");

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Lifecycle', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, PW_HASH);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Other', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                OTHER_CUSTOMER_EMAIL, PW_HASH);

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'LCT001', 'Lifecycle Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, branch_id, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Lifecycle', 'Admin', 'BRANCH_ADMIN', ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ADMIN_EMAIL, PW_HASH, java.util.UUID.fromString(BRANCH_ID));

        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            jdbcTemplate.update(
                    "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                    "VALUES (RANDOM_UUID(), ?, ?, '08:00', '17:00', false)",
                    BRANCH_ID, day.name());
        }
    }

    @Test
    void cancel_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/00000000-0000-4000-8000-000000000099/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void cancel_appointmentNotFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/00000000-0000-4000-8000-000000000099/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void cancel_ownPendingAppointment_returns200() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "No longer needed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = OTHER_CUSTOMER_EMAIL, roles = "CUSTOMER")
    void cancel_otherCustomerAppointment_returns403() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void confirm_pendingAppointment_returns200() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void confirm_byCustomer_returns403() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void complete_confirmedAppointment_returns200() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "CONFIRMED");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void complete_pendingAppointment_returns422() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/complete"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void noShow_confirmedAppointment_returns200() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "CONFIRMED");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/no-show"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NO_SHOW"));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void noShow_byCustomer_returns403() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "CONFIRMED");

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/no-show"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void getHistory_ownAppointment_returns200() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(get("/api/v1/appointments/" + appointmentId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = OTHER_CUSTOMER_EMAIL, roles = "CUSTOMER")
    void getHistory_otherCustomerAppointment_returns403() throws Exception {
        String appointmentId = createAppointmentDirectly(CUSTOMER_EMAIL, "PENDING");

        mockMvc.perform(get("/api/v1/appointments/" + appointmentId + "/history"))
                .andExpect(status().isForbidden());
    }

    private String createAppointmentDirectly(String customerEmail, String status) {
        String id = java.util.UUID.randomUUID().toString();
        String futureDate = nextFutureWeekday();
        jdbcTemplate.update(
                "INSERT INTO appointments (id, customer_id, branch_id, service_id, appointment_date, start_time, end_time, status, reference_number, version, created_at, updated_at) " +
                "SELECT ?, u.id, ?, ?, CAST(? AS DATE), '09:00', '09:15', ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users u WHERE u.email = ?",
                id, BRANCH_ID, SERVICE_ID, futureDate, status, "CAP-2027-" + id.substring(0, 5).toUpperCase(), customerEmail);
        return id;
    }

    private String nextFutureWeekday() {
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
        return date.toString();
    }
}
