package com.capitec.capibook.user;

import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.user.dto.UpdateProfileRequest;
import com.capitec.capibook.user.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhoneNumber("0821234567");
        testUser.setRole(Role.CUSTOMER);
    }

    @Test
    void getProfile_whenUserExists_returnsProfileResponse() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UserProfileResponse profile = userService.getProfile("test@example.com");

        assertThat(profile.email()).isEqualTo("test@example.com");
        assertThat(profile.firstName()).isEqualTo("John");
        assertThat(profile.lastName()).isEqualTo("Doe");
        assertThat(profile.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void getProfile_whenUserDoesNotExist_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesFieldsAndReturnsResponse() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "0829876543");
        UserProfileResponse result = userService.updateProfile("test@example.com", request);

        verify(userRepository).save(testUser);
        assertThat(testUser.getFirstName()).isEqualTo("Jane");
        assertThat(testUser.getLastName()).isEqualTo("Smith");
        assertThat(testUser.getPhoneNumber()).isEqualTo("0829876543");
    }

    @Test
    void updateProfile_whenUserDoesNotExist_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile("ghost@example.com",
                new UpdateProfileRequest("A", "B", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
