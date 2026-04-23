package com.univer.voting.service;

import com.univer.voting.models.Users;
import com.univer.voting.models.VoteOtp;
import com.univer.voting.repository.UserRepository;
import com.univer.voting.repository.VoteOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final VoteOtpRepository otpRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Transactional
    public void sendOtp(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Delete any existing OTPs for this user
        otpRepository.deleteByUserId(userId);

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", new SecureRandom().nextInt(999999));

        // Save OTP with 1 minute expiry
        VoteOtp otp = VoteOtp.builder()
                .userId(userId)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1))
                .build();

        otpRepository.save(otp);

        // Send email
        emailService.sendOtpEmail(user, otpCode);
        log.info("OTP sent to user: {}", userId);
    }

    public void validateOtp(UUID userId, String otpCode) {
        VoteOtp otp = otpRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new IllegalStateException("No OTP found. Please request a new one."));


        System.out.println("^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^");
        System.out.println(otp.getOtpCode());
        System.out.println(otpCode);
        if (otp.getUsed()) {
            throw new IllegalStateException("OTP has already been used. Please request a new one.");
        }

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(otp.getExpiresAt())) {
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            throw new IllegalStateException("Invalid OTP. Please try again.");
        }

        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);
        log.info("OTP validated successfully for user: {}", userId);
    }
}
