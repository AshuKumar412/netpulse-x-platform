package com.netpulse.auth.service;

import com.netpulse.auth.entity.EmailVerificationOtp;
import com.netpulse.auth.repository.EmailVerificationOtpRepository;
import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.user.entity.User;
import com.netpulse.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(EmailVerificationOtpRepository otpRepository,
                                    UserRepository userRepository,
                                    EmailService emailService,
                                    PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendOtp(User user) {
        String email = user.getEmail().trim().toLowerCase();
        Instant now = Instant.now();

        // Invalidate all previous unused OTPs for this email
        List<EmailVerificationOtp> existingOtps = otpRepository.findAllByEmailAndUsedFalse(email);
        for (EmailVerificationOtp oldOtp : existingOtps) {
            oldOtp.setUsed(true);
        }
        otpRepository.saveAll(existingOtps);

        // Generate cryptographically secure 6-digit OTP
        int otpNumber = secureRandom.nextInt(1_000_000);
        String rawOtp = String.format("%06d", otpNumber);

        // Securely hash OTP before saving
        String otpHash = passwordEncoder.encode(rawOtp);

        Instant expiresAt = now.plus(Duration.ofMinutes(OTP_EXPIRATION_MINUTES));
        Instant resendAvailableAt = now.plus(Duration.ofSeconds(RESEND_COOLDOWN_SECONDS));

        EmailVerificationOtp otpRecord = new EmailVerificationOtp(
                user.getId(),
                email,
                otpHash,
                expiresAt,
                resendAvailableAt
        );

        otpRepository.save(otpRecord);
        log.info("OTP_GENERATED email={} userId={} expiresAt={}", email, user.getId(), expiresAt);

        // Send OTP email (rawOtp is only passed in memory to email service, never logged or stored plaintext)
        emailService.sendOtpEmail(email, user.getName(), rawOtp);
    }

    @Transactional
    public void verifyOtp(String email, String rawOtp) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + normalizedEmail));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Account is already verified. Please proceed to login.");
        }

        EmailVerificationOtp otpRecord = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No active verification code found. Please request a new one."));

        Instant now = Instant.now();

        // 1. Check expiration
        if (now.isAfter(otpRecord.getExpiresAt())) {
            otpRecord.setUsed(true);
            otpRepository.save(otpRecord);
            log.warn("OTP_EXPIRED email={}", normalizedEmail);
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }

        // 2. Check maximum attempts
        if (otpRecord.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRecord.setUsed(true);
            otpRepository.save(otpRecord);
            log.warn("OTP_MAX_ATTEMPTS_EXCEEDED email={}", normalizedEmail);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new code.");
        }

        // 3. Increment attempt count
        otpRecord.incrementAttemptCount();

        // 4. Verify OTP hash match
        if (!passwordEncoder.matches(rawOtp, otpRecord.getOtpHash())) {
            otpRepository.save(otpRecord);
            int remaining = MAX_ATTEMPTS - otpRecord.getAttemptCount();
            log.warn("OTP_VERIFICATION_FAILED email={} attemptsUsed={}", normalizedEmail, otpRecord.getAttemptCount());
            if (remaining > 0) {
                throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
            } else {
                otpRecord.setUsed(true);
                otpRepository.save(otpRecord);
                throw new BadRequestException("Invalid verification code. Maximum attempts exceeded. Please request a new code.");
            }
        }

        // 5. Verification successful
        otpRecord.setUsed(true);
        otpRecord.setVerifiedAt(now);
        otpRepository.save(otpRecord);

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("OTP_VERIFICATION_SUCCESS email={} userId={}", normalizedEmail, user.getId());

        // 6. Send Welcome Email strictly after successful verification
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        log.info("WELCOME_EMAIL_SENT email={} userId={}", normalizedEmail, user.getId());
    }

    @Transactional
    public void resendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + normalizedEmail));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Account is already verified. Please proceed to login.");
        }

        EmailVerificationOtp latestOtp = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElse(null);

        Instant now = Instant.now();
        if (latestOtp != null && now.isBefore(latestOtp.getResendAvailableAt())) {
            long waitSeconds = Duration.between(now, latestOtp.getResendAvailableAt()).getSeconds() + 1;
            log.warn("OTP_RESEND_RATE_LIMITED email={} waitSeconds={}", normalizedEmail, waitSeconds);
            throw new BadRequestException("Please wait " + waitSeconds + " seconds before requesting a new code.");
        }

        log.info("OTP_RESEND_REQUESTED email={}", normalizedEmail);
        generateAndSendOtp(user);
    }
}
