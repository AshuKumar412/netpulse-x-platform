package com.netpulse.auth.repository;

import com.netpulse.auth.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    List<EmailVerificationOtp> findAllByEmailAndUsedFalse(String email);

    List<EmailVerificationOtp> findAllByUserId(Long userId);

    void deleteByExpiresAtBefore(Instant cutoff);
}
