package com.capitec.capibook.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AuthIntegrationTest {

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
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void fullFlow_registerLoginAccessRefreshLogout() throws Exception {
        // Register
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "flow@example.com",
                                "password", "Password1!",
                                "firstName", "Flow",
                                "lastName", "Test"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
                .andReturn();

        JsonNode registerData = objectMapper.readTree(registerResult.getResponse().getContentAsString()).path("data");
        String accessToken = registerData.path("accessToken").asText();
        String refreshToken = registerData.path("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // Access protected endpoint
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("flow@example.com"));

        // Refresh tokens
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data");
        String newAccessToken = refreshData.path("accessToken").asText();
        String newRefreshToken = refreshData.path("refreshToken").asText();

        assertThat(newAccessToken).isNotBlank();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Old refresh token is now revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        // Logout revokes new refresh token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newRefreshToken))))
                .andExpect(status().isNoContent());

        // Revoked token can no longer be used
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void security_unauthenticatedAccessToProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void security_tamperedJwt_returns401() throws Exception {
        String accessToken = registerAndGetAccessToken("tamper@example.com");
        String tampered = accessToken.substring(0, accessToken.lastIndexOf('.') + 1) + "invalidsig";

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void security_customerCannotSelfAssignAdminRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "selfadmin@example.com",
                                "password", "Password1!",
                                "firstName", "Self",
                                "lastName", "Admin"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String role = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("user").path("role").asText();
        assertThat(role).isEqualTo("CUSTOMER");
    }

    @Test
    void security_loginRevokesOldRefreshTokens() throws Exception {
        String regRefreshToken = registerAndGetRefreshToken("distinct@example.com");

        // Login issues new tokens
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "distinct@example.com",
                                "password", "Password1!"
                        ))))
                .andExpect(status().isOk());

        // Registration refresh token should be revoked after login
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", regRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileUpdate_persistsChanges() throws Exception {
        String accessToken = registerAndGetAccessToken("persist@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Persisted",
                                "lastName", "Value",
                                "phoneNumber", "0821111111"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Persisted"))
                .andExpect(jsonPath("$.data.lastName").value("Value"));
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1!",
                                "firstName", "Test",
                                "lastName", "User"
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String registerAndGetRefreshToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1!",
                                "firstName", "Test",
                                "lastName", "User"
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
    }
}
