package com.capitec.capibook.branch;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class BranchControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        jdbcTemplate.execute("DELETE FROM branch_operating_hours");
        jdbcTemplate.execute("DELETE FROM branches");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createBranch_withValidRequest_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "branchCode", "CPT001",
                "name", "Cape Town Main",
                "address", "1 Adderley Street",
                "city", "Cape Town",
                "province", "Western Cape",
                "postalCode", "8001"
        );

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("CPT001"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void createBranch_asCustomer_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "branchCode", "JHB001",
                "name", "Joburg Main",
                "address", "1 Commissioner Street",
                "city", "Johannesburg",
                "province", "Gauteng",
                "postalCode", "2001"
        );

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBranch_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createBranch_withInvalidPostalCode_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "branchCode", "CPT002",
                "name", "Cape Town Main",
                "address", "1 Adderley Street",
                "city", "Cape Town",
                "province", "Western Cape",
                "postalCode", "ABC"
        );

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createBranch_withDuplicateBranchCode_returns409() throws Exception {
        Map<String, Object> body = Map.of(
                "branchCode", "CPT003",
                "name", "Cape Town Main",
                "address", "1 Adderley Street",
                "city", "Cape Town",
                "province", "Western Cape",
                "postalCode", "8001"
        );
        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void listBranches_asCustomer_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listBranches_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void getBranchById_withExistingId_returns200() throws Exception {
        String branchId = createBranch("CPT004");

        mockMvc.perform(get("/api/v1/branches/" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branchCode").value("CPT004"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void getBranchById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/branches/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void updateBranch_withValidRequest_returns200() throws Exception {
        String branchId = createBranch("CPT005");

        Map<String, Object> update = Map.of(
                "name", "Updated Name",
                "address", "New Address",
                "city", "Durban",
                "province", "KwaZulu-Natal",
                "postalCode", "4001"
        );

        mockMvc.perform(put("/api/v1/branches/" + branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.city").value("Durban"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void deactivateBranch_returns204() throws Exception {
        String branchId = createBranch("CPT006");

        mockMvc.perform(delete("/api/v1/branches/" + branchId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void deactivatedBranch_doesNotAppearInList() throws Exception {
        String branchId = createBranch("CPT007");
        mockMvc.perform(delete("/api/v1/branches/" + branchId));

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.branchCode == 'CPT007')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void updateOperatingHours_withValidHours_returns200() throws Exception {
        String branchId = createBranch("CPT008");

        List<Map<String, Object>> hours = List.of(
                Map.of("dayOfWeek", "MONDAY", "openTime", "08:00", "closeTime", "17:00", "closed", false),
                Map.of("dayOfWeek", "SATURDAY", "closed", true)
        );

        mockMvc.perform(put("/api/v1/branches/" + branchId + "/operating-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatingHours").isArray());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void updateOperatingHours_withMissingTimesOnOpenDay_returns400() throws Exception {
        String branchId = createBranch("CPT009");

        List<Map<String, Object>> hours = List.of(
                Map.of("dayOfWeek", "MONDAY", "closed", false)
        );

        mockMvc.perform(put("/api/v1/branches/" + branchId + "/operating-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours)))
                .andExpect(status().isBadRequest());
    }

    private String createBranch(String branchCode) throws Exception {
        Map<String, Object> body = Map.of(
                "branchCode", branchCode,
                "name", "Test Branch " + branchCode,
                "address", "1 Test Street",
                "city", "Cape Town",
                "province", "Western Cape",
                "postalCode", "8001"
        );
        MvcResult result = mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
