package com.univer.voting.service;

import com.univer.voting.config.JwtTokenProvider;
import com.univer.voting.dto.request.LoginRequest;
import com.univer.voting.dto.request.RefreshTokenRequest;
import com.univer.voting.dto.response.AuthResponse;
import com.univer.voting.dto.response.UserResponse;
import com.univer.voting.exception.UnauthorizedException;
import com.univer.voting.models.Session;
import com.univer.voting.models.Users;
import com.univer.voting.repository.SessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final SessionRepository sessionRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            Users user = getUserFromLoginRequest(request.getUsernameOrEmail());

            validateUserCanLogin(user);

            String accessToken = tokenProvider.generateToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            saveSession(user, accessToken, refreshToken, ipAddress, userAgent);

            userService.handleSuccessfulLogin(user.getId());

            log.info("User logged in successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
                    .user(mapToUserResponse(user))
                    .build();

        } catch (BadCredentialsException ex) {
            userService.handleFailedLogin(request.getUsernameOrEmail());
            throw new UnauthorizedException("Invalid username or password");
        } catch (DisabledException ex) {
            throw new UnauthorizedException("Account not activated. Please activate your account first.");
        } catch (LockedException ex) {
            throw new UnauthorizedException("Account locked due to multiple failed login attempts. Contact admin.");
        } catch (AuthenticationException ex) {
            log.error("Authentication failed: {}", ex.getMessage());
            throw new UnauthorizedException("Authentication failed");
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        Users user = userService.getUserById(userId);

        Session session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (!session.getIsActive() || session.isExpired()) {
            throw new UnauthorizedException("Session expired. Please login again.");
        }

        String newAccessToken = tokenProvider.generateToken(user);

        session.setSessionToken(newAccessToken);
        session.updateActivity();
        sessionRepository.save(session);

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(UUID userId, String accessToken) {

        sessionRepository.findByUserIdAndSessionToken(userId, accessToken)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    sessionRepository.save(session);
                    log.info("User logged out: {}", userId);
                });
    }

    @Transactional
    public void logoutAll(UUID userId) {

        sessionRepository.deactivateAllUserSessions(userId);
        log.info("User logged out from all devices: {}", userId);
    }


    public Users validateTokenAndGetUser(String token) {
        if (!tokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(token);
        return userService.getUserById(userId);
    }


    private Users getUserFromLoginRequest(String usernameOrEmail) {
        return userService.getUserByUsername(usernameOrEmail);
    }


    private void validateUserCanLogin(Users user) {
        if (!user.getAccountActivated()) {
            throw new UnauthorizedException("Account not activated");
        }
        if (user.getAccountLocked()) {
            throw new UnauthorizedException("Account locked");
        }
    }


    private void saveSession(Users user, String accessToken, String refreshToken,
                             String ipAddress, String userAgent) {

        Session session = Session.builder()
                .user(user)
                .sessionToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .isActive(true)
                .build();

        sessionRepository.save(session);
    }

    private UserResponse mapToUserResponse(Users user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .accountActivated(user.getAccountActivated())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
