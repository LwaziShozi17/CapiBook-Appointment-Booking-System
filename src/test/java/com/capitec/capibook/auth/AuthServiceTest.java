package com.capitec.capibook.auth;

import com.capitec.capibook.auth.dto.AuthResponse;
import com.capitec.capibook.auth.dto.LoginRequest;
import com.capitec.capibook.auth.dto.RegisterRequest;
import com.capitec.capibook.exception.DuplicateEmailException;
import com.capitec.capibook.exception.InvalidTokenException;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604_800_000L);

        testUser = new User();
        testUser.setEmail("user@example.com");
        testUser.setPasswordHash("$2a$10$hash");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.CUSTOMER);
        testUser.setActive(true);
    }

    @Test
    void register_withNewEmail_createsCustomerAccount() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test", "User", null);
        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().role()).isEqualTo("CUSTOMER");
    }

    @Test
    void register_withDuplicateEmail_throwsDuplicateEmailException() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@example.com", "password123", "Test", "User", null)))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_roleIsAlwaysCustomer() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest request = new RegisterRequest("admin@example.com", "password", "Hacker", "Evil", null);
        AuthResponse response = authService.register(request);

        assertThat(response.user().role()).isEqualTo("CUSTOMER");
    }

    @Test
    void login_withValidCredentials_returnsTokens() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_withBadCredentials_propagatesException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_withValidToken_returnsNewAccessToken() {
        RefreshToken token = validRefreshToken();
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh("valid-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void refresh_withNonExistentToken_throwsInvalidTokenException() {
        when(refreshTokenRepository.findByToken("fake-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("fake-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_withRevokedToken_throwsInvalidTokenException() {
        RefreshToken token = validRefreshToken();
        token.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_withExpiredToken_throwsInvalidTokenException() {
        RefreshToken token = validRefreshToken();
        token.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logout_revokesRefreshToken() {
        RefreshToken token = validRefreshToken();
        when(refreshTokenRepository.findByToken("logout-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenReturn(token);

        authService.logout("logout-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    private RefreshToken validRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        return token;
    }
}
