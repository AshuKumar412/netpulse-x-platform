package com.netpulse.auth.service;

import com.netpulse.auth.entity.EmailVerificationOtp;
import com.netpulse.auth.repository.EmailVerificationOtpRepository;
import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.user.entity.Role;
import com.netpulse.user.entity.User;
import com.netpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationOtpRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
                otpRepository,
                userRepository,
                emailService,
                passwordEncoder
        );

        testUser = new User("Bob Engineer", "bob@netpulse.io", "encodedPassword", Role.OPERATOR, false);
        testUser.setId(10L);
    }

    @Test
    @DisplayName("Should generate 6-digit OTP, hash it, save to DB and send OTP email")
    void testGenerateAndSendOtp() {
        when(otpRepository.findAllByEmailAndUsedFalse("bob@netpulse.io")).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp_value");

        emailVerificationService.generateAndSendOtp(testUser);

        ArgumentCaptor<EmailVerificationOtp> captor = ArgumentCaptor.forClass(EmailVerificationOtp.class);
        verify(otpRepository).save(captor.capture());
        EmailVerificationOtp savedOtp = captor.getValue();

        assertEquals(10L, savedOtp.getUserId());
        assertEquals("bob@netpulse.io", savedOtp.getEmail());
        assertEquals("hashed_otp_value", savedOtp.getOtpHash());
        assertFalse(savedOtp.isUsed());
        assertTrue(savedOtp.getExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("bob@netpulse.io"), eq("Bob Engineer"), otpCaptor.capture());
        String sentOtp = otpCaptor.getValue();
        assertNotNull(sentOtp);
        assertEquals(6, sentOtp.length());
        assertTrue(sentOtp.matches("^[0-9]{6}$"));
    }

    @Test
    @DisplayName("Should successfully verify valid OTP, enable account, and send Welcome Email")
    void testVerifyValidOtp() {
        EmailVerificationOtp activeOtp = new EmailVerificationOtp(
                10L,
                "bob@netpulse.io",
                "hashed_otp",
                Instant.now().plus(Duration.ofMinutes(10)),
                Instant.now().plus(Duration.ofSeconds(60))
        );

        when(userRepository.findByEmail("bob@netpulse.io")).thenReturn(Optional.of(testUser));
        when(otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("bob@netpulse.io"))
                .thenReturn(Optional.of(activeOtp));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);

        emailVerificationService.verifyOtp("bob@netpulse.io", "123456");

        assertTrue(activeOtp.isUsed());
        assertNotNull(activeOtp.getVerifiedAt());
        assertTrue(testUser.isEmailVerified());

        verify(otpRepository).save(activeOtp);
        verify(userRepository).save(testUser);
        verify(emailService).sendWelcomeEmail("bob@netpulse.io", "Bob Engineer");
    }

    @Test
    @DisplayName("Should reject invalid OTP and increment attempt count")
    void testVerifyInvalidOtp() {
        EmailVerificationOtp activeOtp = new EmailVerificationOtp(
                10L,
                "bob@netpulse.io",
                "hashed_otp",
                Instant.now().plus(Duration.ofMinutes(10)),
                Instant.now().plus(Duration.ofSeconds(60))
        );

        when(userRepository.findByEmail("bob@netpulse.io")).thenReturn(Optional.of(testUser));
        when(otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("bob@netpulse.io"))
                .thenReturn(Optional.of(activeOtp));
        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                emailVerificationService.verifyOtp("bob@netpulse.io", "999999"));

        assertTrue(ex.getMessage().contains("Invalid verification code"));
        assertEquals(1, activeOtp.getAttemptCount());
        assertFalse(testUser.isEmailVerified());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should reject expired OTP and mark record as used")
    void testVerifyExpiredOtp() {
        EmailVerificationOtp expiredOtp = new EmailVerificationOtp(
                10L,
                "bob@netpulse.io",
                "hashed_otp",
                Instant.now().minus(Duration.ofMinutes(1)), // Expired
                Instant.now().minus(Duration.ofMinutes(2))
        );

        when(userRepository.findByEmail("bob@netpulse.io")).thenReturn(Optional.of(testUser));
        when(otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("bob@netpulse.io"))
                .thenReturn(Optional.of(expiredOtp));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                emailVerificationService.verifyOtp("bob@netpulse.io", "123456"));

        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(expiredOtp.isUsed());
        verify(otpRepository).save(expiredOtp);
    }

    @Test
    @DisplayName("Should reject resend request during active 60s cooldown")
    void testResendOtpCooldownEnforced() {
        EmailVerificationOtp activeOtp = new EmailVerificationOtp(
                10L,
                "bob@netpulse.io",
                "hashed_otp",
                Instant.now().plus(Duration.ofMinutes(9)),
                Instant.now().plus(Duration.ofSeconds(45)) // Cooldown active for 45s
        );

        when(userRepository.findByEmail("bob@netpulse.io")).thenReturn(Optional.of(testUser));
        when(otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("bob@netpulse.io"))
                .thenReturn(Optional.of(activeOtp));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                emailVerificationService.resendOtp("bob@netpulse.io"));

        assertTrue(ex.getMessage().contains("Please wait"));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should allow resend OTP once cooldown has elapsed")
    void testResendOtpAfterCooldownElapsed() {
        EmailVerificationOtp pastOtp = new EmailVerificationOtp(
                10L,
                "bob@netpulse.io",
                "hashed_otp",
                Instant.now().plus(Duration.ofMinutes(5)),
                Instant.now().minus(Duration.ofSeconds(5)) // Cooldown expired
        );

        when(userRepository.findByEmail("bob@netpulse.io")).thenReturn(Optional.of(testUser));
        when(otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("bob@netpulse.io"))
                .thenReturn(Optional.of(pastOtp));
        when(otpRepository.findAllByEmailAndUsedFalse("bob@netpulse.io")).thenReturn(List.of(pastOtp));
        when(passwordEncoder.encode(anyString())).thenReturn("new_hash");

        emailVerificationService.resendOtp("bob@netpulse.io");

        verify(emailService).sendOtpEmail(eq("bob@netpulse.io"), eq("Bob Engineer"), anyString());
    }
}
