package com.univer.voting.service;

import com.univer.voting.enums.RegistrationType;
import com.univer.voting.enums.UserRole;
import com.univer.voting.exception.BadRequestException;
import com.univer.voting.exception.ResourceNotFoundException;
import com.univer.voting.models.Users;
import com.univer.voting.repository.ElectionRepository;
import com.univer.voting.repository.UserRepository;
import com.univer.voting.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/**
 * User Import Service
 * Handles CSV import, bulk operations, and statistics
 *
 * @author Otabek To'lqinov
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService {

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final VoteRepository voteRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    /**
     * Import users from CSV file
     * CSV Format: username,email,fullName,nationalId
     *
     * @param file CSV file
     * @param importedBy Admin who is importing
     * @return Import results (success count, errors)
     */
    @Transactional
    public Map<String, Object> importUsersFromCSV(MultipartFile file, UUID importedBy) {

        List<Users> importedUsers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            // Parse CSV with Apache Commons CSV
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            for (CSVRecord record : csvParser) {
                try {
                    // Extract fields from CSV
                    String username = record.get("username");
                    String email = record.get("email");
                    String fullName = record.get("fullName");
                    String nationalId = record.get("nationalId");

                    // Validate fields
                    if (username == null || username.isBlank()) {
                        errors.add("Row " + record.getRecordNumber() + ": Username is required");
                        errorCount++;
                        continue;
                    }

                    if (email == null || email.isBlank()) {
                        errors.add("Row " + record.getRecordNumber() + ": Email is required");
                        errorCount++;
                        continue;
                    }

                    if (fullName == null || fullName.isBlank()) {
                        errors.add("Row " + record.getRecordNumber() + ": Full name is required");
                        errorCount++;
                        continue;
                    }

                    if (nationalId == null || nationalId.isBlank()) {
                        errors.add("Row " + record.getRecordNumber() + ": National ID is required");
                        errorCount++;
                        continue;
                    }

                    // Check for duplicates
                    if (userRepository.existsByUsername(username)) {
                        errors.add("Row " + record.getRecordNumber() + ": Username '" + username + "' already exists");
                        errorCount++;
                        continue;
                    }

                    if (userRepository.existsByEmail(email)) {
                        errors.add("Row " + record.getRecordNumber() + ": Email '" + email + "' already exists");
                        errorCount++;
                        continue;
                    }

                    if (userRepository.existsByNationalId(nationalId)) {
                        errors.add("Row " + record.getRecordNumber() + ": National ID '" + nationalId + "' already exists");
                        errorCount++;
                        continue;
                    }

                    // Create user
                    Users user = Users.builder()
                            .username(username.trim())
                            .email(email.trim().toLowerCase())
                            .fullName(fullName.trim())
                            .nationalId(nationalId.trim())
                            .passwordHash(null) // No password until activation
                            .accountActivated(false)
                            .emailVerified(false)
                            .activationToken(UUID.randomUUID())
                            .role(UserRole.VOTER)
                            .registrationType(RegistrationType.IMPORTED)
                            .importedBy(importedBy)
                            .build();

                    Users savedUser = userRepository.save(user);
                    importedUsers.add(savedUser);

                    // Send activation email
                    try {
                        emailService.sendActivationEmail(savedUser);
                    } catch (Exception emailEx) {
                        log.error("Failed to send activation email to: {}", email, emailEx);
                        // Don't fail the import if email fails
                    }

                    successCount++;
                    log.debug("Imported user: {}", username);

                } catch (Exception ex) {
                    log.error("Error importing user at row {}", record.getRecordNumber(), ex);
                    errors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                    errorCount++;
                }
            }

            // Log import action
            auditService.logUserImport(importedBy, importedBy);

            log.info("CSV import completed: {} success, {} errors", successCount, errorCount);

        } catch (Exception ex) {
            log.error("Failed to parse CSV file", ex);
            throw new BadRequestException("Failed to parse CSV file: " + ex.getMessage());
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("importedUsers", importedUsers);
        result.put("errors", errors);

        return result;
    }

    /**
     * Resend activation email to user
     */
    @Transactional
    public void resendActivationEmail(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getAccountActivated()) {
            throw new BadRequestException("Account already activated");
        }

        if (user.getActivationToken() == null) {
            // Generate new token if missing
            user.setActivationToken(UUID.randomUUID());
            userRepository.save(user);
        }

        emailService.sendActivationEmail(user);

        log.info("Resent activation email to: {}", user.getEmail());
    }

    /**
     * Get users pending activation
     */
    public List<Users> getPendingActivationUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.getAccountActivated() && user.getActivationToken() != null)
                .toList();
    }

    /**
     * Get system statistics
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(Users::getAccountActivated)
                .count();
        long pendingActivation = userRepository.findAll().stream()
                .filter(user -> !user.getAccountActivated())
                .count();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("pendingActivation", pendingActivation);

        // Election statistics
        long totalElections = electionRepository.count();
        long activeElections = electionRepository.findActiveElections(
                java.time.LocalDateTime.now()
        ).size();

        stats.put("totalElections", totalElections);
        stats.put("activeElections", activeElections);

        // Vote statistics
        long totalVotes = voteRepository.count();
        stats.put("totalVotes", totalVotes);

        return stats;
    }

    /**
     * Get detailed user statistics
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Users> allUsers = userRepository.findAll();

        // Count by role
        long voters = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.VOTER)
                .count();
        long admins = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
        long electionOfficers = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.ELECTION_OFFICER)
                .count();

        stats.put("totalVoters", voters);
        stats.put("totalAdmins", admins);
        stats.put("totalElectionOfficers", electionOfficers);

        // Count by registration type
        long selfRegistered = allUsers.stream()
                .filter(u -> u.getRegistrationType() == RegistrationType.SELF)
                .count();
        long imported = allUsers.stream()
                .filter(u -> u.getRegistrationType() == RegistrationType.IMPORTED)
                .count();

        stats.put("selfRegistered", selfRegistered);
        stats.put("imported", imported);

        // Account status
        long activated = allUsers.stream()
                .filter(Users::getAccountActivated)
                .count();
        long emailVerified = allUsers.stream()
                .filter(Users::getEmailVerified)
                .count();
        long locked = allUsers.stream()
                .filter(Users::getAccountLocked)
                .count();

        stats.put("activatedAccounts", activated);
        stats.put("emailVerifiedAccounts", emailVerified);
        stats.put("lockedAccounts", locked);

        return stats;
    }
}
