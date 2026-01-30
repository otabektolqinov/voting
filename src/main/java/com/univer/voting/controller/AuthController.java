package com.univer.voting.controller;

import com.univer.voting.config.JwtTokenProvider;
import com.univer.voting.dto.request.*;
import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.dto.response.AuthResponse;
import com.univer.voting.dto.response.UserResponse;
import com.univer.voting.service.AuthenticationService;
import com.univer.voting.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Registration attempt for username: {}", request.getUsername());

        UserResponse user = userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful! Please check your email to verify your account.",
                        user
                ));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authenticationService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("Token refresh request");

        AuthResponse response = authenticationService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            HttpServletRequest httpRequest
    ) {
        String token = extractTokenFromRequest(httpRequest);

        if (token == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No authentication token provided"));
        }

        // Get user ID directly from token
        UUID userId = tokenProvider.getUserIdFromToken(token);

        // Logout
        authenticationService.logout(userId, token);

        log.info("User logged out: {}", userId);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully")
        );
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse> logoutAll(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());

        authenticationService.logoutAll(userId);

        log.info("User logged out from all devices: {}", userId);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out from all devices successfully")
        );
    }


    @PostMapping("/activate")
    public ResponseEntity<ApiResponse> activateAccount(
            @Valid @RequestBody ActivateAccountRequest request
    ) {
        log.info("Account activation attempt with token: {}", request.getActivationToken());

        UserResponse user = userService.activateAccount(request);

        return ResponseEntity.ok(
                ApiResponse.success("Account activated successfully! You can now login.", user)
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(
            @RequestParam("token") String token
    ) {
        log.info("Email verification attempt with token: {}", token);

        userService.verifyEmail(token);

        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully!")
        );
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        log.info("Password reset requested for email: {}", request.getEmail());

        // TODO: Implement password reset in UserService
        // userService.requestPasswordReset(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "If an account exists with that email, a password reset link has been sent."
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody NewPasswordRequest request
    ) {
        log.info("Password reset attempt with token: {}", request.getResetToken());

        // TODO: Implement password reset in UserService
        return ResponseEntity.ok(
                ApiResponse.success("Password reset successfully! You can now login with your new password.")
        );
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Token is valid", userDetails.getUsername())
        );
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
