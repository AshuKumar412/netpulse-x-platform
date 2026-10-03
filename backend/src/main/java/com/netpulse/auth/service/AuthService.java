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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists: " + normalizedEmail);
        }

        // The first registered user automatically becomes ADMIN, subsequent users default to OPERATOR
        Role role = (userRepository.count() == 0) ? Role.ADMIN : Role.OPERATOR;

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                role,
                false // Email is unverified upon initial registration
        );

        User savedUser = userRepository.save(user);

        // Generate and send 6-digit OTP email
        emailVerificationService.generateAndSendOtp(savedUser);

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        emailVerificationService.verifyOtp(request.getEmail(), request.getOtp());
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        emailVerificationService.resendOtp(request.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Enforce mandatory email verification before allowing login
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Email not verified. Please verify your email before logging in.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);
            return new AuthResponse(token, UserResponse.fromEntity(user));
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }
}
