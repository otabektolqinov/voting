package com.univer.voting.service;

import com.univer.voting.enums.AuditAction;
import com.univer.voting.enums.SeverityLevel;
import com.univer.voting.models.AuditLog;
import com.univer.voting.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logLogin(UUID userId) {
        createAuditLog(userId, null, AuditAction.LOGIN,
                "User logged in", SeverityLevel.INFO);
    }

    public void logLogout(UUID userId) {
        createAuditLog(userId, null, AuditAction.LOGOUT,
                "User logged out", SeverityLevel.INFO);
    }

    public void logUserRegistration(UUID userId) {
        createAuditLog(userId, null, AuditAction.USER_REGISTER,
                "User registered (self)", SeverityLevel.INFO);
    }

    public void logUserImport(UUID userId, UUID importedBy) {
        createAuditLog(userId, null, AuditAction.USER_REGISTER,
                "User imported by admin: " + importedBy, SeverityLevel.INFO);
    }

    public void logAccountActivation(UUID userId) {
        createAuditLog(userId, null, AuditAction.ACCOUNT_ACTIVATED,
                "Account activated", SeverityLevel.INFO);
    }

    public void logAccountLocked(UUID userId) {
        createAuditLog(userId, null, AuditAction.ADMIN_ACTION,
                "Account locked due to failed login attempts", SeverityLevel.WARNING);
    }

    public void logRoleChange(UUID userId, String oldRole, String newRole) {
        createAuditLog(userId, null, AuditAction.ADMIN_ACTION,
                "Role changed from " + oldRole + " to " + newRole, SeverityLevel.INFO);
    }

    public void logUserDeletion(UUID userId) {
        createAuditLog(userId, null, AuditAction.ADMIN_ACTION,
                "User deleted", SeverityLevel.WARNING);
    }

    public void logElectionCreated(UUID userId, UUID electionId) {
        createAuditLog(userId, electionId, AuditAction.ELECTION_CREATE,
                "Election created", SeverityLevel.INFO);
    }

    public void logElectionClosed(UUID userId, UUID electionId) {
        createAuditLog(userId, electionId, AuditAction.ELECTION_CLOSE,
                "Election closed", SeverityLevel.INFO);
    }

    public void logVoteCast(UUID userId, UUID electionId) {
        createAuditLog(userId, electionId, AuditAction.VOTE_CAST,
                "Vote cast", SeverityLevel.INFO);
    }

    public void logCandidateAdded(UUID userId, UUID electionId, String candidateName) {
        createAuditLog(userId, electionId, AuditAction.CANDIDATE_ADD,
                "Candidate added: " + candidateName, SeverityLevel.INFO);
    }

    private void createAuditLog(UUID userId, UUID electionId, AuditAction action,
                                String description, SeverityLevel severity) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .electionId(electionId)
                .actionType(action)
                .actionDescription(description)
                .severity(severity)
                .build();

        auditLogRepository.save(log);
        /*log.debug("Audit log created: {} - {}", action, description);*/ // todo: Needs to be set up
    }
}
