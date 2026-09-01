package com.capitec.capibook.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AvailabilityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BRANCH_ID = "aaaaaaaa-0000-4000-8000-000000000001";
    // Card Collection service seeded in V7: 15-min duration
    private static final String SERVICE_ID = "00000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        jdbcTemplate.execute("DELETE FROM appointments");
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches");

        jdbcTemplate.update(
                "INSERT INTO branches (id, branch_code, name, address, city, province, postal_code, active, max_concurrent_appointments, created_at, updated_at) " +
                "VALUES (?, 'CPT001', 'Cape Town Main', '1 Adderley Street', 'Cape Town', 'Western Cape', '8001', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                BRANCH_ID
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAvailability_onWeekdayWithHours_returnsSlots() throws Exception {
        // Monday 2025-06-09 is not a holiday; seed operating hours for MONDAY 08:00–09:00
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'MONDAY', '08:00', '09:00', false)",
                BRANCH_ID
        );

        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", BRANCH_ID)
                        .param("serviceId", SERVICE_ID)
                        .param("date", "2025-06-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slots").isArray())
                // 08:00–09:00 with 15-min slots → 4 slots
                .andExpect(jsonPath("$.data.slots.length()").value(4))
                .andExpect(jsonPath("$.data.slots[0].startTime").value("08:00:00"))
                .andExpect(jsonPath("$.data.slots[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.slots[3].startTime").value("08:45:00"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAvailability_onPublicHoliday_returnsEmptySlots() throws Exception {
        // 2025-12-25 is Christmas Day — seeded in V10
        jdbcTemplate.update(
                "INSERT INTO branch_operating_hours (id, branch_id, day_of_week, open_time, close_time, closed) " +
                "VALUES (RANDOM_UUID(), ?, 'THURSDAY', '08:00', '17:00', false)",
                BRANCH_ID
        );

        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", BRANCH_ID)
                        .param("serviceId", SERVICE_ID)
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots").isEmpty());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAvailability_whenBranchClosedThatDay_returnsEmptySlots() throws Exception {
        // No operating hours record for MONDAY → branch considered closed
        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", BRANCH_ID)
                        .param("serviceId", SERVICE_ID)
                        .param("date", "2025-06-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots").isEmpty());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAvailability_withUnknownBranch_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", "bbbbbbbb-0000-4000-8000-000000000001")
                        .param("serviceId", SERVICE_ID)
                        .param("date", "2025-06-09"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAvailability_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", BRANCH_ID)
                        .param("serviceId", SERVICE_ID)
                        .param("date", "2025-06-09"))
                .andExpect(status().isUnauthorized());
    }
}
