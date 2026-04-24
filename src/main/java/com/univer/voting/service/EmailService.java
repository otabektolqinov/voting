package com.univer.voting.service;

import com.univer.voting.models.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${voting.email.from}")
    private String fromEmail;

    @Value("${voting.email.from-name}")
    private String fromName;

    @Value("${spring.application.name}")
    private String appName;

    public void sendActivationEmail(Users user) {
        String subject = "Activate Your Voting Account";
        String activationLink = "http://91.99.215.33:9090/activate?token=" + user.getActivationToken();

        String body = String.format("""
            Dear %s,
            
            Your voting account has been created by the system administrator.
            
            To complete your registration and activate your account, please click the link below:
            %s
            
            This link will expire in 7 days.
            
            If you did not expect this email, please ignore it.
            
            Best regards,
            %s Team
            """, user.getFullName(), activationLink, appName);

        sendEmail(user.getEmail(), subject, body);
        log.info("Activation email sent to: {}", user.getEmail());
    }

    public void sendEmailVerification(Users user) {
        String subject = "Verify Your Email Address";
        String verificationLink = "http://91.99.215.33:9090/api/auth/verify-email?token=" + user.getVerificationToken();

        String body = String.format("""
            Dear %s,
            
            Please verify your email address by clicking the link below:
            %s
            
            This link will expire in 24 hours.
            
            Best regards,
            %s Team
            """, user.getFullName(), verificationLink, appName);

        sendEmail(user.getEmail(), subject, body);
        log.info("Verification email sent to: {}", user.getEmail());
    }

    public void sendPasswordResetEmail(Users user, String resetToken) {
        String subject = "Reset Your Password";
        String resetLink = "http://91.99.215.33:9090/reset-password?token=" + resetToken;

        String body = String.format("""
            Dear %s,
            
            You requested to reset your password.
            
            Click the link below to set a new password:
            %s
            
            This link will expire in 1 hour.
            
            If you did not request this, please ignore this email.
            
            Best regards,
            %s Team
            """, user.getFullName(), resetLink, appName);

        sendEmail(user.getEmail(), subject, body);
        log.info("Password reset email sent to: {}", user.getEmail());
    }

    /**
     * Generic email sender
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.debug("Email sent successfully to: {}", to);
        } catch (Exception ex) {
            log.error("Failed to send email to: {}", to, ex);
            throw new RuntimeException("Email sending failed", ex);
        }
    }

    public void sendOtpEmail(Users user, String otpCode) {
        String subject = "Your Voting OTP Code";

        String body = String.format("""
        Dear %s,
        
        Your OTP code to confirm your vote is:
        
        %s
        
        This code expires in 1 minute.
        
        If you did not request this, please ignore this email.
        
        Best regards,
        %s Team
        """, user.getFullName(), otpCode, appName);

        sendEmail(user.getEmail(), subject, body);
        log.info("OTP email sent to: {}", user.getEmail());
    }
}
