package com.capitec.capibook.auth;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"app.security.rate-limit.auth-requests-per-minute=5"})
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void rateLimiting_loginEndpoint_returns429AfterLimit() throws Exception {
        // 5 requests allowed, 6th should be throttled
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "10.1.0.1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "any@example.com",
                            "password", "wrongpass"
                    ))));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.1.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "any@example.com",
                                "password", "wrongpass"
                        ))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimiting_registerEndpoint_returns429AfterLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                    .header("X-Forwarded-For", "10.1.0.2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "rate" + i + "@example.com",
                            "password", "Password1!",
                            "firstName", "Test",
                            "lastName", "User"
                    ))));
        }

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", "10.1.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "rate5@example.com",
                                "password", "Password1!",
                                "firstName", "Test",
                                "lastName", "User"
                        ))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void bruteForce_locksAfterFiveFailedAttempts() throws Exception {
        registerUser("brute@example.com");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "10.2.0.1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "brute@example.com",
                            "password", "WrongPassword!"
                    ))))
                    .andExpect(status().isUnauthorized());
        }

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT failed_login_attempts FROM users WHERE email = 'brute@example.com'",
                Integer.class);
        assertThat(attempts).isGreaterThanOrEqualTo(5);

        Object lockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM users WHERE email = 'brute@example.com'",
                Object.class);
        assertThat(lockedUntil).isNotNull();
    }

    @Test
    void bruteForce_lockedAccount_rejectsCorrectPassword() throws Exception {
        registerUser("locked@example.com");

        // Directly lock the account via JDBC to avoid rate limit interference
        jdbcTemplate.update(
                "UPDATE users SET failed_login_attempts = 5, locked_until = DATEADD('MINUTE', 15, CURRENT_TIMESTAMP()) " +
                "WHERE email = 'locked@example.com'");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.2.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "locked@example.com",
                                "password", "Password1!"
                        ))))
                .andExpect(status().isLocked());
    }

    @Test
    void bruteForce_lockoutExpiry_allowsLogin() throws Exception {
        registerUser("expired@example.com");

        jdbcTemplate.update(
                "UPDATE users SET failed_login_attempts = 5, locked_until = DATEADD('MINUTE', -1, CURRENT_TIMESTAMP()) " +
                "WHERE email = 'expired@example.com'");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.2.0.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "expired@example.com",
                                "password", "Password1!"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void bruteForce_successfulLoginResetsFailedCounter() throws Exception {
        registerUser("reset@example.com");

        jdbcTemplate.update("UPDATE users SET failed_login_attempts = 3 WHERE email = 'reset@example.com'");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.2.0.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reset@example.com",
                                "password", "Password1!"
                        ))))
                .andExpect(status().isOk());

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT failed_login_attempts FROM users WHERE email = 'reset@example.com'",
                Integer.class);
        assertThat(attempts).isEqualTo(0);

        Object lockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM users WHERE email = 'reset@example.com'",
                Object.class);
        assertThat(lockedUntil).isNull();
    }

    @Test
    void securityHeaders_presentOnAllResponses() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.3.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "headers@example.com",
                                "password", "Password1!"
                        ))))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void inputValidation_passwordTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", "10.3.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "short@example.com",
                                "password", "abc",
                                "firstName", "Test",
                                "lastName", "User"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters"));
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .header("X-Forwarded-For", "10.0.0.99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", "Password1!",
                        "firstName", "Test",
                        "lastName", "User"
                ))));
    }
}
