package com.univer.voting.controller;

import com.univer.voting.dto.request.ImportUserRequest;
import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.models.Users;
import com.univer.voting.service.UserImportService;
import com.univer.voting.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserImportService userImportService;
    private final UserService userService;


    @PostMapping("/users/import")
    public ResponseEntity<ApiResponse> importUsers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String adminId
    ) {
        log.info("User import started by admin: {}", adminId);

        if (file.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("File is empty"));
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Only CSV files are supported"));
        }

        try {
            Map<String, Object> result = userImportService.importUsersFromCSV(
                    file,
                    UUID.fromString(adminId)
            );

            int successCount = (int) result.get("successCount");
            int errorCount = (int) result.get("errorCount");
            List<String> errors = (List<String>) result.get("errors");

            String message = String.format(
                    "Import completed: %d users imported successfully, %d errors",
                    successCount, errorCount
            );

            log.info("User import completed: {} success, {} errors", successCount, errorCount);

            return ResponseEntity.ok(
                    ApiResponse.success(message, result)
            );

        } catch (Exception ex) {
            log.error("Error importing users", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to import users: " + ex.getMessage()));
        }
    }

    @PostMapping("/users/import-single")
    public ResponseEntity<ApiResponse> importSingleUser(
            @Valid @RequestBody ImportUserRequest request,
            @AuthenticationPrincipal String adminId
    ) {
        log.info("Importing single user: {} by admin: {}", request.getUsername(), adminId);

        Users user = userService.importUser(
                request.getUsername(),
                request.getEmail(),
                request.getFullName(),
                request.getNationalId(),
                UUID.fromString(adminId)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User imported successfully. Activation email sent.",
                        user
                ));
    }

    @PostMapping("/users/{userId}/resend-activation")
    public ResponseEntity<ApiResponse> resendActivationEmail(
            @PathVariable UUID userId
    ) {
        log.info("Resending activation email for user: {}", userId);

        userImportService.resendActivationEmail(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Activation email resent successfully")
        );
    }

    @GetMapping("/users/pending-activation")
    public ResponseEntity<ApiResponse> getPendingActivationUsers() {
        log.debug("Getting users pending activation");

        List<Users> users = userImportService.getPendingActivationUsers();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pending activation users retrieved",
                        users
                )
        );
    }

    @GetMapping("/users/import-template")
    public ResponseEntity<String> getImportTemplate() {
        String template = "username,email,fullName,nationalId\n" +
                "student001,student001@university.uz,Ali Karimov,STU-2024-001\n" +
                "student002,student002@university.uz,Dilnoza Yusupova,STU-2024-002\n" +
                "student003,student003@university.uz,Bobur Tursunov,STU-2024-003\n";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=user-import-template.csv")
                .header("Content-Type", "text/csv")
                .body(template);
    }


    @PostMapping("/users/bulk-unlock")
    public ResponseEntity<ApiResponse> bulkUnlockAccounts(
            @RequestBody List<UUID> userIds
    ) {
        log.info("Bulk unlocking {} accounts", userIds.size());

        int unlockedCount = 0;
        for (UUID userId : userIds) {
            try {
                userService.unlockAccount(userId);
                unlockedCount++;
            } catch (Exception ex) {
                log.error("Failed to unlock account: {}", userId, ex);
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Unlocked %d out of %d accounts", unlockedCount, userIds.size())
                )
        );
    }

    @PostMapping("/users/bulk-delete")
    public ResponseEntity<ApiResponse> bulkDeleteUsers(
            @RequestBody List<UUID> userIds
    ) {
        log.warn("Bulk deleting {} users", userIds.size());

        int deletedCount = 0;
        for (UUID userId : userIds) {
            try {
                userService.deleteUser(userId);
                deletedCount++;
            } catch (Exception ex) {
                log.error("Failed to delete user: {}", userId, ex);
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Deleted %d out of %d users", deletedCount, userIds.size())
                )
        );
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<ApiResponse> getSystemStats() {
        log.debug("Getting system statistics");

        Map<String, Object> stats = userImportService.getSystemStatistics();

        return ResponseEntity.ok(
                ApiResponse.success("System statistics", stats)
        );
    }

    @GetMapping("/stats/users")
    public ResponseEntity<ApiResponse> getUserStats() {
        log.debug("Getting user statistics");

        Map<String, Object> stats = userImportService.getUserStatistics();

        return ResponseEntity.ok(
                ApiResponse.success("User statistics", stats)
        );
    }
}

