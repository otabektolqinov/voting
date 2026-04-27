package com.univer.voting.service;

import com.univer.voting.dto.request.ActivateAccountRequest;
import com.univer.voting.dto.request.RegisterRequest;
import com.univer.voting.dto.response.UserResponse;
import com.univer.voting.enums.RegistrationType;
import com.univer.voting.enums.UserRole;
import com.univer.voting.exception.BadRequestException;
import com.univer.voting.exception.ResourceNotFoundException;
import com.univer.voting.models.Users;
import com.univer.voting.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${voting.self-registration-enabled}")
    private boolean selfRegistrationEnabled;

    @Value("${voting.max-failed-login-attempts}")
    private int maxFailedLoginAttempts;

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        if (!selfRegistrationEnabled) {
            throw new BadRequestException("Self-registration is currently disabled");
        }

        validateUserUniqueness(request.getUsername(), request.getEmail(), request.getNationalId());

        Users user = Users.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .nationalId(request.getNationalId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountActivated(true)
                .emailVerified(false)
                .verificationToken(UUID.randomUUID())
                .role(UserRole.VOTER)
                .registrationType(RegistrationType.SELF)
                .build();

        Users savedUser = userRepository.save(user);

        emailService.sendEmailVerification(savedUser);

        auditService.logUserRegistration(savedUser.getId());

        log.info("User registered successfully: {}", savedUser.getUsername());

        return mapToUserResponse(savedUser);
    }


    @Transactional
    public Users importUser(String username, String email, String fullName,
                           String nationalId, UUID importedBy) {

        validateUserUniqueness(username, email, nationalId);

        Users user = Users.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .nationalId(nationalId)
                .passwordHash(null)
                .accountActivated(false)
                .emailVerified(false)
                .activationToken(UUID.randomUUID())
                .role(UserRole.VOTER)
                .registrationType(RegistrationType.IMPORTED)
                .importedBy(importedBy)
                .build();

        Users savedUser = userRepository.save(user);

        emailService.sendActivationEmail(savedUser);

        auditService.logUserImport(savedUser.getId(), importedBy);

        log.info("User imported successfully: {}", savedUser.getUsername());

        return savedUser;
    }

    @Transactional
    public UserResponse activateAccount(ActivateAccountRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        UUID activationToken = UUID.fromString(request.getActivationToken());
        Users user = userRepository.findByActivationToken(activationToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired activation token"));

        if (user.getAccountActivated()) {
            throw new BadRequestException("Account already activated");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAccountActivated(true);
        user.setEmailVerified(true);
        user.setActivationToken(null);
        user.setUpdatedAt(LocalDateTime.now());

        Users activatedUser = userRepository.save(user);

        auditService.logAccountActivation(activatedUser.getId());

        log.info("Account activated successfully: {}", activatedUser.getUsername());

        return mapToUserResponse(activatedUser);
    }

    public UserResponse toggleActivation(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountActivated(!user.getAccountActivated());
        userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public void verifyEmail(String verificationToken) {
        UUID token = UUID.fromString(verificationToken);

        Users user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);

        userRepository.save(user);

        log.info("Email verified for user: {}", user.getUsername());
    }


    public Users getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Users getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    public List<UserResponse> getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Users> users = userRepository.findByEmailContainingIgnoreCaseAndRole(
                email.trim(),
                UserRole.VOTER
        );

        List<UserResponse> results = users.stream()
                .limit(10)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .username(user.getUsername())
                        .build())
                .toList();
        return results;
    }


    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLogin(String usernameOrEmail) {
        Users user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElse(null);

        if (user != null) {
            user.incrementFailedLoginAttempts(maxFailedLoginAttempts);
            userRepository.save(user);

            if (user.getAccountLocked()) {
                log.warn("Account locked due to failed login attempts: {}", user.getUsername());
                auditService.logAccountLocked(user.getId());
            }
        }
    }

    @Transactional
    public void handleSuccessfulLogin(UUID userId) {
        Users user = getUserById(userId);
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        auditService.logLogin(userId);
        log.info("User logged in successfully: {}", user.getUsername());
    }

    @Transactional
    public void unlockAccount(UUID userId) {
        Users user = getUserById(userId);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.info("Account unlocked: {}", user.getUsername());
    }

    @Transactional
    public UserResponse updateUserRole(UUID userId, UserRole newRole) {
        Users user = getUserById(userId);
        UserRole oldRole = user.getRole();

        user.setRole(newRole);
        Users updatedUser = userRepository.save(user);

        auditService.logRoleChange(userId, oldRole.name(), newRole.name());
        log.info("User role updated: {} -> {}", oldRole, newRole);

        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        Users user = getUserById(userId);
        userRepository.delete(user);

        auditService.logUserDeletion(userId);
        log.info("User deleted: {}", user.getUsername());
    }

    private void validateUserUniqueness(String username, String email, String nationalId) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByNationalId(nationalId)) {
            throw new BadRequestException("National ID already registered");
        }
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
