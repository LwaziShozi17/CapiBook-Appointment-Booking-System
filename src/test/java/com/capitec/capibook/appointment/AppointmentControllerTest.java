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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "cccccccc-0000-4000-8000-000000000001";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001"; // Card Collection (15 min)
    private static final String CUSTOMER_EMAIL = "controller_customer@test.com";
    private static final String CUSTOMER_PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"; // "password"

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches");
        jdbcTemplate.execute("DELETE FROM users WHERE email = '" + CUSTOMER_EMAIL + "'");

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Controller', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, CUSTOMER_PW_HASH
        );

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'CTR001', 'Controller Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID
        );

        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'MONDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'TUESDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'WEDNESDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'THURSDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'FRIDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );
    }

    @Test
    void createAppointment_withoutAuthentication_returns401() throws Exception {
        Map<String, Object> body = Map.of(
                "branchId", BRANCH_ID,
                "serviceId", SERVICE_ID,
                "appointmentDate", "2027-06-09",
                "startTime", "09:00:00"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "BRANCH_ADMIN")
    void createAppointment_withBranchAdminRole_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "branchId", BRANCH_ID,
                "serviceId", SERVICE_ID,
                "appointmentDate", "2027-06-09",
                "startTime", "09:00:00"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void createAppointment_withMissingBranchId_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "serviceId", SERVICE_ID,
                "appointmentDate", "2027-06-09",
                "startTime", "09:00:00"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void createAppointment_withPastDate_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "branchId", BRANCH_ID,
                "serviceId", SERVICE_ID,
                "appointmentDate", "2020-01-01",
                "startTime", "09:00:00"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void createAppointment_validRequest_returns201() throws Exception {
        // Find a future weekday that is not a public holiday
        String futureMonday = nextFutureNonHolidayWeekday();

        Map<String, Object> body = Map.of(
                "branchId", BRANCH_ID,
                "serviceId", SERVICE_ID,
                "appointmentDate", futureMonday,
                "startTime", "09:00:00"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.referenceNumber").value(org.hamcrest.Matchers.startsWith("CAP-")))
                .andExpect(jsonPath("$.data.branchName").value("Controller Branch"));
    }

    @Test
    void getAppointmentById_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/00000000-0000-4000-8000-000000000099"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void getMyAppointments_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "BRANCH_ADMIN")
    void getMyAppointments_withBranchAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/my"))
                .andExpect(status().isForbidden());
    }

    private String nextFutureNonHolidayWeekday() {
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        // Skip known 2026 holidays (all are in 2026 per the seed data)
        java.util.Set<java.time.LocalDate> holidays = java.util.Set.of(
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 3, 21),
                java.time.LocalDate.of(2026, 4, 3),
                java.time.LocalDate.of(2026, 4, 6),
                java.time.LocalDate.of(2026, 4, 27),
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 6, 16),
                java.time.LocalDate.of(2026, 8, 9),
                java.time.LocalDate.of(2026, 9, 24),
                java.time.LocalDate.of(2026, 12, 16),
                java.time.LocalDate.of(2026, 12, 25),
                java.time.LocalDate.of(2026, 12, 26)
        );
        while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY
                || holidays.contains(date)) {
            date = date.plusDays(1);
        }
        return date.toString();
    }
}
