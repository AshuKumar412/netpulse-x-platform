package com.netpulse.auth.service;

import com.netpulse.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@netpulse-x.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:NetPulse X Platform}")
    private String fromName;

    public SmtpEmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String recipientName, String otp) {
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "User";
        String subject = "Verify your NetPulse X account";
        String body = String.format("""
                Hello %s,

                Welcome to NetPulse X.

                Your email verification code is:

                %s

                This code expires in 10 minutes.

                If you did not create this account, you can safely ignore this email.

                Regards,
                NetPulse X
                """, name, otp);

        sendMail(toEmail, subject, body, "OTP_EMAIL");
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName) {
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "User";
        String subject = "Welcome to NetPulse X";
        String body = String.format("""
                Hello %s,

                Your NetPulse X account has been successfully verified.

                You can now sign in and access the NetPulse X operations platform.

                Regards,
                NetPulse X
                """, name);

        sendMail(toEmail, subject, body, "WELCOME_EMAIL");
    }

    private void sendMail(String toEmail, String subject, String body, String type) {
        Instant timestamp = Instant.now();
        if (mailSender == null) {
            log.warn("JavaMailSender not configured. Email delivery simulated for {} to {} at {}", type, toEmail, timestamp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("EMAIL_SENT type={} to={} timestamp={}", type, toEmail, timestamp);
        } catch (Exception ex) {
            log.error("EMAIL_DELIVERY_FAILED type={} to={} timestamp={} error={}", type, toEmail, timestamp, ex.getMessage());
            throw new EmailDeliveryException("Failed to deliver " + type + " to " + toEmail + ". Please check email settings or try again.", ex);
        }
    }
}
