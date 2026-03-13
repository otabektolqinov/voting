package com.univer.voting.service;

import com.univer.voting.dto.request.CandidateRequest;
import com.univer.voting.dto.request.CreateElectionRequest;
import com.univer.voting.dto.request.UpdateElectionRequest;
import com.univer.voting.dto.response.ElectionDTO;
import com.univer.voting.dto.response.ElectionResultDTO;
import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.enums.ElectionType;
import com.univer.voting.exception.BadRequestException;
import com.univer.voting.exception.ResourceNotFoundException;
import com.univer.voting.models.Candidate;
import com.univer.voting.models.Election;
import com.univer.voting.models.ElectionVoter;
import com.univer.voting.repository.*;
import com.univer.voting.service.mapper.ElectionMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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
    private final UserRepository userRepository;
    private final ElectionMapper electionMapper;

    /**
     * Create new election (DRAFT status)
     */
    @Transactional
    public Election createElection(CreateElectionRequest request,
                                   UUID createdBy) {

        Election election = new Election();

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        if (request.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Start date must be in the future");
        }

        if (request.getIsPublic() || request.getVoterIds().isEmpty()) {
            election.setType(ElectionType.PUBLIC);
        } else {
            election.setType(ElectionType.RESTRICTED);
            for (String voterId : request.getVoterIds()) {
                ElectionVoter ev = new ElectionVoter();
                ev.setElection(election);
                ev.setUser(userRepository.findById(UUID.fromString(voterId)).orElseThrow());
                ev.setAddedBy(createdBy);
                electionVoterRepository.save(ev);
            }
        }

        election.setTitle(request.getTitle());
        election.setDescription(request.getDescription());
        election.setStartDate(request.getStartDate());
        election.setEndDate(request.getEndDate());
        election.setCreatedBy(createdBy);
        election.setStatus(ElectionStatus.DRAFT);

        Election saved = electionRepository.save(election);

        List<Candidate> savedCandidates = new ArrayList<>();
        for (CandidateRequest candidateReq : request.getCandidates()) {
            Candidate candidate = new Candidate();
            candidate.setElection(saved);
            candidate.setName(candidateReq.getName());
            candidate.setPartyAffiliation(candidateReq.getPartyAffiliation());
            candidate.setBio(candidateReq.getBio());
            candidate.setPhotoUrl(candidateReq.getPhotoUrl());

            Candidate candidate1 = candidateRepository.save(candidate);
            savedCandidates.add(candidate1);
            log.debug("Created candidate: {} for election: {}", candidate1.getName(), election.getId());
        }

        auditService.logElectionCreated(createdBy, saved.getId());

        log.info("Successfully created election '{}' with {} candidates and {} eligible voters",
                election.getTitle(), savedCandidates.size(), request.getVoterIds().size());

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
        return electionRepository.findAllPublicElection();
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
    public List<ElectionDTO> getElectionsForUser(UUID userId) {
        /*List<Election> activeElections = getActiveElections();

        return activeElections.stream()
                *//*.filter(election -> canUserVoteInElection(userId, election.getId()))*//*
                .toList();*/
        log.info("Getting elections for voter: {}", userId);

        // STEP 1: Get all elections this voter can see
        List<Election> elections = electionRepository.findElectionsForVoter(userId);

        log.info("Found {} elections for voter", elections.size());

        // STEP 2: Convert to DTOs and add hasVoted flag
        List<ElectionDTO> electionDTOs = new ArrayList<>();

        for (Election election : elections) {
            // Check if voter has voted in this election
            boolean hasVoted = voteRepository.existsByElectionIdAndUserId(
                    election.getId(),
                    userId
            );

            // Convert to DTO
            ElectionDTO dto = electionMapper.toDTO(election);

            // Add hasVoted flag
            dto.setHasVoted(hasVoted);

            electionDTOs.add(dto);
        }

        log.info("Returning {} elections with hasVoted flags", electionDTOs.size());

        return electionDTOs;
    }

    @Transactional
    public Election updateElection(UUID electionId, UpdateElectionRequest request) {

        Election election = getElectionById(electionId);

        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Can only update elections in DRAFT status");
        }

        if (request.getTitle() != null) election.setTitle(request.getTitle());
        if (request.getDescription() != null) election.setDescription(request.getDescription());
        if (request.getStartDate() != null) election.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) election.setEndDate(request.getEndDate());

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
    public List<ElectionResultDTO> getElectionResults(UUID electionId) {
        log.info("Getting results for election: {}", electionId);

        // STEP 1: Get the election
        Election election = getElectionById(electionId);

        // STEP 2: Validate - can we show results?
        // Option A: Only if election is CLOSED
        // Option B: Allow viewing anytime (for testing)
        // Let's use Option B for now (you can change this later)

        if (election.getStatus() == ElectionStatus.DRAFT) {
            throw new BadRequestException("Cannot view results - election hasn't started yet");
        }

        // STEP 3: Get vote counts from database
        // This returns: [[candidateId, voteCount], [candidateId, voteCount], ...]
        List<Object[]> voteCounts = voteRepository.countVotesByCandidate(electionId);

        // Convert to a Map for easy lookup: candidateId -> voteCount
        Map<UUID, Long> voteCountMap = new HashMap<>();
        for (Object[] row : voteCounts) {
            UUID candidateId = (UUID) row[0];
            Long count = (Long) row[1];
            voteCountMap.put(candidateId, count);
        }

        // STEP 4: Calculate total votes
        Long totalVotes = voteCountMap.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        log.info("Total votes for election {}: {}", electionId, totalVotes);

        // STEP 5: Build result DTOs for each candidate
        List<ElectionResultDTO> results = new ArrayList<>();

        for (Candidate candidate : election.getCandidates()) {
            Long candidateVotes = voteCountMap.getOrDefault(candidate.getId(), 0L);

            // Calculate percentage
            Double percentage = 0.0;
            if (totalVotes > 0) {
                percentage = (candidateVotes * 100.0) / totalVotes;
            }

            // Build the DTO
            ElectionResultDTO resultDTO = ElectionResultDTO.builder()
                    .candidateId(candidate.getId().toString())
                    .candidateName(candidate.getName())
                    .partyAffiliation(candidate.getPartyAffiliation())
                    .voteCount(candidateVotes)
                    .percentage(percentage)
                    .build();

            results.add(resultDTO);
        }

        // STEP 6: Sort by vote count (highest first)
        results.sort((a, b) -> Long.compare(b.getVoteCount(), a.getVoteCount()));

        // STEP 7: Assign ranks (1st, 2nd, 3rd, etc.)
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        log.info("Returning {} candidate results", results.size());

        return results;
    }


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

        /*if (voteRepository.countByElectionIdAndIsValid(electionId, true) > 0) {
            throw new BadRequestException("Cannot delete election with votes");
        }*/

        electionRepository.delete(election);

        log.info("Election deleted: {}", election.getTitle());
    }

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
