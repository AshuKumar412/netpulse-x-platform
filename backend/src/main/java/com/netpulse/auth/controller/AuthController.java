package com.netpulse.auth.controller;

import com.netpulse.auth.dto.AuthResponse;
import com.netpulse.auth.dto.LoginRequest;
import com.netpulse.auth.dto.RegisterRequest;
import com.netpulse.auth.dto.ResendOtpRequest;
import com.netpulse.auth.dto.VerifyEmailRequest;
import com.netpulse.auth.service.AuthService;
import com.netpulse.common.ApiResponse;
import com.netpulse.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, email OTP verification, and JWT authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new unverified user account and dispatches a 6-digit OTP verification email")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email with the 6-digit OTP code sent to your inbox.", response));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email with OTP", description = "Verifies the 6-digit OTP code and enables account login")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now sign in.", null));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend email verification OTP", description = "Generates and sends a new 6-digit verification code with a 60-second cooldown")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("A new verification code has been sent to your email.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates verified user credentials and returns a signed JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
