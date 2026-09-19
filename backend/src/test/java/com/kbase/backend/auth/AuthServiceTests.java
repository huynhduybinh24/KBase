package com.kbase.backend.auth;

import com.kbase.backend.auth.dto.AuthResponse;
import com.kbase.backend.auth.dto.LoginRequest;
import com.kbase.backend.auth.dto.RegisterRequest;
import com.kbase.backend.exception.EmailAlreadyExistsException;
import com.kbase.backend.exception.InvalidCredentialsException;
import com.kbase.backend.security.JwtService;
import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserProfileService profileService;

    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                profileService
        );
        lenient().when(jwtService.getExpirationSeconds()).thenReturn(3600L);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsUserRole() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("Person Example", " Person@Example.com ", "password123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("person@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", savedUser.passwordHash()));
        assertEquals(Set.of(Role.USER), savedUser.getRoles());
        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(profileService).createForRegistration(savedUser, "Person Example");
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(
                        new RegisterRequest(null, "person@example.com", "password123"))
        );
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User(
                "person@example.com",
                passwordEncoder.encode("password123"),
                Set.of(Role.USER)
        );
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(userRepository.findByEmailIgnoreCase("person@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(
                new LoginRequest("PERSON@example.com", "password123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals(Set.of("USER"), response.user().roles());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(
                new User("person@example.com", passwordEncoder.encode("password123"), Set.of(Role.USER))));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest("person@example.com", "wrong-password"))
        );
    }

    @Test
    void googleOnlyUserCannotUsePasswordLogin() {
        when(userRepository.findByEmailIgnoreCase("google@example.com"))
                .thenReturn(Optional.of(new User("google@example.com", null, Set.of(Role.USER))));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("google@example.com", "anything")));
        verify(authenticationManager, org.mockito.Mockito.never()).authenticate(any());
    }
}
