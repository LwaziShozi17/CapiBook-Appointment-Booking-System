package com.capitec.capibook.servicecatalog;

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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ServiceCatalogControllerTest {

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
        jdbcTemplate.execute("DELETE FROM banking_services WHERE id NOT LIKE '00000000-0000-4000-8000-%'");
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void listServices_asCustomer_returns200WithSeededServices() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(12));
    }

    @Test
    void listServices_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createService_withValidRequest_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "New Test Service",
                "description", "A new service",
                "durationMinutes", 30
        );

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Test Service"))
                .andExpect(jsonPath("$.data.durationMinutes").value(30))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void createService_asCustomer_returns403() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Unauthorised Service",
                "durationMinutes", 15
        );

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createService_withZeroDuration_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Invalid Service",
                "durationMinutes", 0
        );

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void createService_withDuplicateName_returns409() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "name", "Duplicate Service",
                "durationMinutes", 20
        ));

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void getServiceById_withExistingId_returns200() throws Exception {
        String serviceId = createService("Loan Consultation");

        mockMvc.perform(get("/api/v1/services/" + serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Loan Consultation"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void getServiceById_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/services/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void updateService_withValidRequest_returns200() throws Exception {
        String serviceId = createService("Service To Update");

        Map<String, Object> update = Map.of(
                "name", "Updated Service Name",
                "description", "Updated description",
                "durationMinutes", 45
        );

        mockMvc.perform(put("/api/v1/services/" + serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Service Name"))
                .andExpect(jsonPath("$.data.durationMinutes").value(45));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void deactivateService_returns204() throws Exception {
        String serviceId = createService("Service To Deactivate");

        mockMvc.perform(delete("/api/v1/services/" + serviceId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SYSTEM_ADMIN")
    void deactivatedService_doesNotAppearInList() throws Exception {
        String serviceId = createService("Deactivated Service");
        mockMvc.perform(delete("/api/v1/services/" + serviceId));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Deactivated Service')]").doesNotExist());
    }

    private String createService(String name) throws Exception {
        Map<String, Object> body = Map.of("name", name, "durationMinutes", 30);
        MvcResult result = mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
