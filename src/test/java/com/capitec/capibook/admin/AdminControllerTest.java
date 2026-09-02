package com.capitec.capibook.admin;

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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AdminControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "cccc1111-0000-4000-8000-000000000001";
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001";
    private static final String SYS_ADMIN_EMAIL = "ctrl_sysadmin@test.com";
    private static final String BRANCH_ADMIN_EMAIL = "ctrl_branchadmin@test.com";
    private static final String CUSTOMER_EMAIL = "ctrl_customer@test.com";
    private static final String PW_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        jdbcTemplate.execute("UPDATE users SET branch_id = NULL WHERE email = '" + BRANCH_ADMIN_EMAIL + "'");
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?, ?)",
                SYS_ADMIN_EMAIL, BRANCH_ADMIN_EMAIL, CUSTOMER_EMAIL);
        jdbcTemplate.execute("DELETE FROM branch_availability_exceptions WHERE branch_id = '" + BRANCH_ID + "'");
        jdbcTemplate.execute("DELETE FROM branch_operating_hours WHERE branch_id = '" + BRANCH_ID + "'");
        jdbcTemplate.execute("DELETE FROM branches WHERE id = '" + BRANCH_ID + "'");

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Sys', 'Admin', 'SYSTEM_ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SYS_ADMIN_EMAIL, PW_HASH);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Ctrl', 'Customer', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                CUSTOMER_EMAIL, PW_HASH);

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'CTL001', 'Ctrl Branch', '1 Test St', 'Cape Town', 'Western Cape', '8001', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role, branch_id, active, created_at, updated_at) " +
                "VALUES (RANDOM_UUID(), ?, ?, 'Ctrl', 'Admin', 'BRANCH_ADMIN', ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ADMIN_EMAIL, PW_HASH, UUID.fromString(BRANCH_ID));
    }

    // ── Security: unauthenticated ──────────────────────────────────────────────

    @Test
    void adminEndpoints_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── User listing ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void listUsers_systemAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void listUsers_customer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void listUsers_branchAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ── Create branch admin ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void createBranchAdmin_systemAdmin_validRequest_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "new_ctrl_admin@test.com",
                "firstName", "New",
                "lastName", "Admin",
                "branchId", BRANCH_ID,
                "password", "password123"
        );

        mockMvc.perform(post("/api/v1/admin/users/branch-admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("BRANCH_ADMIN"));

        jdbcTemplate.update("DELETE FROM users WHERE email = 'new_ctrl_admin@test.com'");
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void createBranchAdmin_customer_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "blocked@test.com",
                "firstName", "Blocked",
                "lastName", "User",
                "branchId", BRANCH_ID,
                "password", "password123"
        );

        mockMvc.perform(post("/api/v1/admin/users/branch-admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ── Deactivate user ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void deactivateUser_systemAdmin_returns200() throws Exception {
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", String.class, CUSTOMER_EMAIL);

        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void deactivateUser_branchAdmin_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + UUID.randomUUID() + "/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void listAppointments_branchAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void listAppointments_systemAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = CUSTOMER_EMAIL, roles = "CUSTOMER")
    void listAppointments_customer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isForbidden());
    }

    // ── Audit logs ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void listAuditLogs_systemAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void listAuditLogs_branchAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void getAppointmentSummary_systemAdmin_returns200() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        mockMvc.perform(get("/api/v1/admin/analytics/appointments/summary")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.periodStart").value(from.toString()));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void getAppointmentSummary_branchAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/appointments/summary")
                        .param("from", LocalDate.now().minusDays(30).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    // ── Branch availability exceptions ────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void createException_systemAdmin_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "exceptionDate", LocalDate.now().plusDays(30).toString(),
                "type", "CLOSED",
                "reason", "Maintenance"
        );

        mockMvc.perform(post("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void listExceptions_branchAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Delete availability exception ─────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void deleteException_systemAdmin_existingException_returns200() throws Exception {
        LocalDate exceptionDate = LocalDate.now().plusDays(60);
        Map<String, Object> body = Map.of(
                "exceptionDate", exceptionDate.toString(),
                "type", "CLOSED",
                "reason", "Test deletion"
        );

        // Create exception first
        String createResponse = mockMvc.perform(post("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String exceptionId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // Delete it
        mockMvc.perform(delete("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions/" + exceptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void deleteException_branchAdmin_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── Branch utilisation analytics ──────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void getBranchUtilisation_systemAdmin_returns200() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        mockMvc.perform(get("/api/v1/admin/analytics/branches/" + BRANCH_ID + "/utilisation")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchId").value(BRANCH_ID));
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void getBranchUtilisation_branchAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/branches/" + BRANCH_ID + "/utilisation")
                        .param("from", LocalDate.now().minusDays(7).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    // ── Service popularity analytics ──────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void getServicePopularity_systemAdmin_returns200() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        mockMvc.perform(get("/api/v1/admin/analytics/services/popularity")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = BRANCH_ADMIN_EMAIL, roles = "BRANCH_ADMIN")
    void getServicePopularity_branchAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/services/popularity")
                        .param("from", LocalDate.now().minusDays(7).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    // ── Duplicate exception ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void createException_duplicate_returns409() throws Exception {
        LocalDate duplicateDate = LocalDate.now().plusDays(90);
        Map<String, Object> body = Map.of(
                "exceptionDate", duplicateDate.toString(),
                "type", "CLOSED",
                "reason", "First"
        );

        mockMvc.perform(post("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/branches/" + BRANCH_ID + "/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // ── Appointments with branch filter (SYSTEM_ADMIN) ────────────────────────

    @Test
    @WithMockUser(username = SYS_ADMIN_EMAIL, roles = "SYSTEM_ADMIN")
    void listAppointments_systemAdmin_withBranchFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments")
                        .param("branchId", BRANCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
