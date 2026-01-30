package com.univer.voting.service;

import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.exception.BadRequestException;
import com.univer.voting.exception.ResourceNotFoundException;
import com.univer.voting.models.Candidate;
import com.univer.voting.models.Election;
import com.univer.voting.models.ElectionVoter;
import com.univer.voting.repository.CandidateRepository;
import com.univer.voting.repository.ElectionRepository;
import com.univer.voting.repository.ElectionVoterRepository;
import com.univer.voting.repository.VoteRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Election Service
 * Handles election creation, management, and lifecycle
 *
 * @author Otabek To'lqinov
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionVoterRepository electionVoterRepository;
    private final VoteRepository voteRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /**
     * Create new election (DRAFT status)
     */
    @Transactional
    public Election createElection(String title, String description,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   UUID createdBy) {

        // Validate dates
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }

        if (startDate.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Start date must be in the future");
        }

        Election election = Election.builder()
                .title(title)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .createdBy(createdBy)
                .status(ElectionStatus.DRAFT)
                .build();

        Election saved = electionRepository.save(election);

        // Log action
        auditService.logElectionCreated(createdBy, saved.getId());

        log.info("Election created: {} by user {}", saved.getTitle(), createdBy);

        return saved;
    }

    /**
     * Get election by ID
     */
    public Election getElectionById(UUID electionId) {
        return electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));
    }

    /**
     * Get all elections
     */
    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    /**
     * Get active elections
     */
    public List<Election> getActiveElections() {
        return electionRepository.findActiveElections(LocalDateTime.now());
    }

    /**
     * Get elections by status
     */
    public List<Election> getElectionsByStatus(ElectionStatus status) {
        return electionRepository.findByStatus(status);
    }

    /**
     * Get elections created by user
     */
    public List<Election> getElectionsByCreator(UUID userId) {
        return electionRepository.findByCreatedBy(userId);
    }

    /**
     * Get elections user can vote in
     */
    public List<Election> getElectionsForUser(UUID userId) {
        List<Election> activeElections = getActiveElections();

        return activeElections.stream()
                .filter(election -> canUserVoteInElection(userId, election.getId()))
                .toList();
    }

    /**
     * Update election details (only if DRAFT)
     */
    @Transactional
    public Election updateElection(UUID electionId, String title, String description,
                                   LocalDateTime startDate, LocalDateTime endDate) {

        Election election = getElectionById(electionId);

        // Can only update DRAFT elections
        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Can only update elections in DRAFT status");
        }

        if (title != null) election.setTitle(title);
        if (description != null) election.setDescription(description);
        if (startDate != null) election.setStartDate(startDate);
        if (endDate != null) election.setEndDate(endDate);

        return electionRepository.save(election);
    }

    /**
     * Add candidate to election
     */
    @Transactional
    public Candidate addCandidate(UUID electionId, String name, String partyAffiliation,
                                  String bio, String photoUrl) {

        Election election = getElectionById(electionId);

        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Can only add candidates to DRAFT elections");
        }

        Candidate candidate = Candidate.builder()
                .election(election)
                .name(name)
                .partyAffiliation(partyAffiliation)
                .bio(bio)
                .photoUrl(photoUrl)
                .displayOrder(candidateRepository.countByElectionId(electionId))
                .build();

        Candidate saved = candidateRepository.save(candidate);

        log.info("Candidate added to election {}: {}", electionId, name);

        return saved;
    }

    /**
     * Get candidates for election
     */
    public List<Candidate> getCandidates(UUID electionId) {
        return candidateRepository.findByElectionIdOrderByDisplayOrder(electionId);
    }

    /**
     * Add eligible voter to election (restricted election)
     */
    @Transactional
    public void addEligibleVoter(UUID electionId, UUID userId, String reason, UUID addedBy) {

        Election election = getElectionById(electionId);

        // Check if already added
        if (electionVoterRepository.findByElectionIdAndUserId(electionId, userId).isPresent()) {
            throw new BadRequestException("User already in eligible voters list");
        }

        ElectionVoter electionVoter = ElectionVoter.builder()
                .election(election)
                .user(null) // Will be set by JPA
                .eligible(true)
                .eligibilityReason(reason)
                .addedBy(addedBy)
                .build();

        // Need to manually set user_id in the entity
        // Or use userRepository.getReferenceById(userId)
        electionVoterRepository.save(electionVoter);

        // Notify user
        // notificationService.notifyEligibilityGranted(userId, election);

        log.info("Added eligible voter {} to election {}", userId, electionId);
    }

    /**
     * Remove eligible voter from election
     */
    @Transactional
    public void removeEligibleVoter(UUID electionId, UUID userId) {
        ElectionVoter electionVoter = electionVoterRepository
                .findByElectionIdAndUserId(electionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Eligible voter not found"));

        electionVoterRepository.delete(electionVoter);

        log.info("Removed eligible voter {} from election {}", userId, electionId);
    }

    /**
     * Get eligible voters for election
     */
    public List<ElectionVoter> getEligibleVoters(UUID electionId) {
        return electionVoterRepository.findByElectionId(electionId);
    }

    /**
     * Activate election (change from DRAFT to ACTIVE)
     */
    @Transactional
    public Election activateElection(UUID electionId) {
        Election election = getElectionById(electionId);

        if (!election.canBeActivated()) {
            throw new BadRequestException("Election cannot be activated. Check candidates and dates.");
        }

        election.setStatus(ElectionStatus.ACTIVE);
        Election activated = electionRepository.save(election);

        // Notify eligible voters
        notifyEligibleVoters(electionId);

        log.info("Election activated: {}", election.getTitle());

        return activated;
    }

    /**
     * Close election (change from ACTIVE to CLOSED)
     */
    @Transactional
    public Election closeElection(UUID electionId, UUID closedBy) {
        Election election = getElectionById(electionId);

        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE elections can be closed");
        }

        election.setStatus(ElectionStatus.CLOSED);
        Election closed = electionRepository.save(election);

        // Log action
        auditService.logElectionClosed(closedBy, electionId);

        log.info("Election closed: {}", election.getTitle());

        return closed;
    }

    /**
     * Publish election results
     */
    @Transactional
    public void publishResults(UUID electionId) {
        Election election = getElectionById(electionId);

        if (election.getStatus() != ElectionStatus.CLOSED) {
            throw new BadRequestException("Can only publish results for CLOSED elections");
        }

        election.setResultsPublished(true);
        electionRepository.save(election);

        // Notify voters
        notifyResultsPublished(electionId);

        log.info("Results published for election: {}", election.getTitle());
    }

    /**
     * Get election results (vote counts per candidate)
     */
    public List<Object[]> getElectionResults(UUID electionId) {
        Election election = getElectionById(electionId);

        // Only show results if published or to admins
        if (!election.getResultsPublished()) {
            throw new BadRequestException("Results not yet published");
        }

        // TODO: Return proper result DTO
        // For now, return candidate vote counts
        return List.of();
    }

    /**
     * Check if election is restricted (has eligible voter list)
     */
    public boolean isRestricted(UUID electionId) {
        return electionVoterRepository.existsByElectionId(electionId);
    }

    /**
     * Check if user can vote in election
     */
    public boolean canUserVoteInElection(UUID userId, UUID electionId) {
        Election election = getElectionById(electionId);

        // Election must be active
        if (!election.isActive()) {
            return false;
        }

        // User must not have voted already
        if (voteRepository.existsByUserIdAndElectionId(userId, electionId)) {
            return false;
        }

        // If election is restricted, user must be in eligible list
        if (isRestricted(electionId)) {
            return electionVoterRepository.findByElectionIdAndUserId(electionId, userId)
                    .map(ElectionVoter::getEligible)
                    .orElse(false);
        }

        // Open election - any verified user can vote
        return true;
    }

    /**
     * Delete election (only if DRAFT and no votes)
     */
    @Transactional
    public void deleteElection(UUID electionId) {
        Election election = getElectionById(electionId);

        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Can only delete DRAFT elections");
        }

        if (voteRepository.countByElectionIdAndIsValid(electionId, true) > 0) {
            throw new BadRequestException("Cannot delete election with votes");
        }

        electionRepository.delete(election);

        log.info("Election deleted: {}", election.getTitle());
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void notifyEligibleVoters(UUID electionId) {
        Election election = getElectionById(electionId);
        List<ElectionVoter> eligibleVoters = getEligibleVoters(electionId);

        for (ElectionVoter ev : eligibleVoters) {
            // notificationService.notifyElectionCreated(ev.getUser(), election);
        }
    }

    private void notifyResultsPublished(UUID electionId) {
        // TODO: Notify all voters
    }
}
