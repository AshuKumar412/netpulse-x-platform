package com.netpulse.auth.service;

import com.netpulse.auth.dto.AuthResponse;
import com.netpulse.auth.dto.LoginRequest;
import com.netpulse.auth.dto.RegisterRequest;
import com.netpulse.auth.dto.ResendOtpRequest;
import com.netpulse.auth.dto.VerifyEmailRequest;
import com.netpulse.exception.DuplicateResourceException;
import com.netpulse.exception.UnauthorizedException;
import com.netpulse.security.JwtTokenProvider;
import com.netpulse.security.UserPrincipal;
import com.netpulse.user.dto.UserResponse;
import com.netpulse.user.entity.Role;
import com.netpulse.user.entity.User;
import com.netpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Alice Engineer", "alice@netpulse.io", "securePass123");
        loginRequest = new LoginRequest("alice@netpulse.io", "securePass123");
    }

    @Test
    @DisplayName("Should successfully register first user as ADMIN and trigger OTP email generation")
    void testRegisterFirstUserAsAdmin() {
        when(userRepository.existsByEmail("alice@netpulse.io")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("securePass123")).thenReturn("hashedPass123");

        User savedUser = new User("Alice Engineer", "alice@netpulse.io", "hashedPass123", Role.ADMIN, false);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("Alice Engineer", response.getName());
        assertEquals("alice@netpulse.io", response.getEmail());
        assertEquals(Role.ADMIN, response.getRole());
        assertFalse(response.isEmailVerified());

        verify(passwordEncoder).encode("securePass123");
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).generateAndSendOtp(savedUser);
    }

    @Test
    @DisplayName("Should register subsequent users as OPERATOR")
    void testRegisterSubsequentUserAsOperator() {
        when(userRepository.existsByEmail("alice@netpulse.io")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("securePass123")).thenReturn("hashedPass123");

        User savedUser = new User("Alice Engineer", "alice@netpulse.io", "hashedPass123", Role.OPERATOR, false);
        savedUser.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(Role.OPERATOR, response.getRole());
        verify(emailVerificationService).generateAndSendOtp(savedUser);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already registered")
    void testRegisterDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("alice@netpulse.io")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).generateAndSendOtp(any());
    }

    @Test
    @DisplayName("Should successfully authenticate verified user and return JWT token")
    void testSuccessfulLogin() {
        User user = new User("Alice Engineer", "alice@netpulse.io", "hashedPass123", Role.ADMIN, true);
        user.setId(1L);
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(userRepository.findByEmail("alice@netpulse.io")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("alice@netpulse.io", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Should reject login when user email is not yet verified")
    void testLoginUnverifiedUserThrowsUnauthorizedException() {
        User user = new User("Alice Engineer", "alice@netpulse.io", "hashedPass123", Role.ADMIN, false);
        user.setId(1L);
        when(userRepository.findByEmail("alice@netpulse.io")).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        assertTrue(ex.getMessage().contains("Email not verified"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when credentials are wrong for verified user")
    void testLoginWithInvalidCredentialsThrowsException() {
        User user = new User("Alice Engineer", "alice@netpulse.io", "hashedPass123", Role.ADMIN, true);
        user.setId(1L);
        when(userRepository.findByEmail("alice@netpulse.io")).thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should delegate email verification to EmailVerificationService")
    void testVerifyEmailDelegation() {
        VerifyEmailRequest request = new VerifyEmailRequest("alice@netpulse.io", "123456");
        authService.verifyEmail(request);
        verify(emailVerificationService).verifyOtp("alice@netpulse.io", "123456");
    }

    @Test
    @DisplayName("Should delegate resend OTP to EmailVerificationService")
    void testResendOtpDelegation() {
        ResendOtpRequest request = new ResendOtpRequest("alice@netpulse.io");
        authService.resendOtp(request);
        verify(emailVerificationService).resendOtp("alice@netpulse.io");
    }
}
