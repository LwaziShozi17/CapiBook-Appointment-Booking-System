package com.capitec.capibook.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long-for-HS256";
    private static final long EXPIRATION_MS = 900_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", EXPIRATION_MS);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        UserDetails userDetails = buildUser("test@example.com");
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectEmail() {
        UserDetails userDetails = buildUser("jane@example.com");
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        UserDetails userDetails = buildUser("valid@example.com");
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        UserDetails owner = buildUser("owner@example.com");
        UserDetails other = buildUser("other@example.com");
        String token = jwtService.generateAccessToken(owner);
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        UserDetails userDetails = buildUser("tamper@example.com");
        String token = jwtService.generateAccessToken(userDetails);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtService.isTokenValid(tampered, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", -1L);
        UserDetails userDetails = buildUser("expired@example.com");
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void getAccessTokenExpirationMs_returnsConfiguredValue() {
        assertThat(jwtService.getAccessTokenExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    private UserDetails buildUser(String email) {
        return new User(email, "hashed", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}
