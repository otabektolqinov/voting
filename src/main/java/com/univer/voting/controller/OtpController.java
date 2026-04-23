package com.univer.voting.controller;

import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('VOTER')")
    public ResponseEntity<ApiResponse> sendOtp(
            @AuthenticationPrincipal String userId
    ) {
        try {
            otpService.sendOtp(UUID.fromString(userId));
            return ResponseEntity.ok(ApiResponse.success("OTP sent to your email"));
        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
