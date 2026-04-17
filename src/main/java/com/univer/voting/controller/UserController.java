package com.univer.voting.controller;

import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.dto.response.UserResponse;
import com.univer.voting.enums.UserRole;
import com.univer.voting.models.Users;
import com.univer.voting.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal String username
    ) {
        log.debug("Getting profile for user: {}", username);

        Users user = userService.getUserByUsername(username);
        UserResponse response = mapToUserResponse(user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id
    ) {
        log.debug("Getting user by ID: {}", id);

        Users user = userService.getUserById(id);
        UserResponse response = mapToUserResponse(user);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("Getting all users (admin request)");

        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(required = false) String email
    ) {
        List<UserResponse> results = userService.getUserByEmail(email);

        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUserRole(
            @PathVariable UUID id,
            @RequestParam UserRole newRole
    ) {
        log.info("Updating role for user {}: {}", id, newRole);

        UserResponse user = userService.updateUserRole(id, newRole);

        return ResponseEntity.ok(
                ApiResponse.success("User role updated successfully", user)
        );
    }


    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> unlockAccount(
            @PathVariable UUID id
    ) {
        log.info("Unlocking account for user: {}", id);

        userService.unlockAccount(id);

        return ResponseEntity.ok(
                ApiResponse.success("Account unlocked successfully")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(
            @PathVariable UUID id
    ) {
        log.warn("Deleting user: {}", id);

        userService.deleteUser(id);

        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully")
        );
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getUserStats() {
        log.debug("Getting user statistics");

        // TODO: Implement statistics
        // Map<String, Object> stats = userService.getUserStatistics();

        return ResponseEntity.ok(
                ApiResponse.success("User statistics", null)
        );
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
