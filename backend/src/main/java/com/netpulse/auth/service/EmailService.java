package com.netpulse.auth.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String recipientName, String otp);

    void sendWelcomeEmail(String toEmail, String recipientName);
}
