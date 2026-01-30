package com.univer.voting.controller;

import com.univer.voting.dto.request.AddCandidateRequest;
import com.univer.voting.dto.request.AddEligibleVoterRequest;
import com.univer.voting.dto.request.CreateElectionRequest;
import com.univer.voting.dto.request.UpdateElectionRequest;
import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.models.Candidate;
import com.univer.voting.models.Election;
import com.univer.voting.service.ElectionService;
import com.univer.voting.models.ElectionVoter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
@Slf4j
public class ElectionController {

    private final ElectionService electionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> createElection(
            @Valid @RequestBody CreateElectionRequest request,
            @AuthenticationPrincipal String userId
    ) {
        log.info("Creating election: {}", request.getTitle());

        Election election = electionService.createElection(
                request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                UUID.fromString(userId)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Election created successfully", election));
    }

    @GetMapping
    public ResponseEntity<List<Election>> getAllElections(
            @RequestParam(required = false) ElectionStatus status
    ) {
        log.debug("Getting all elections with status: {}", status);

        List<Election> elections = status != null
                ? electionService.getElectionsByStatus(status)
                : electionService.getAllElections();

        return ResponseEntity.ok(elections);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Election>> getActiveElections() {
        log.debug("Getting active elections");

        List<Election> elections = electionService.getActiveElections();

        return ResponseEntity.ok(elections);
    }

    @GetMapping("/my-elections")
    public ResponseEntity<List<Election>> getMyElections(
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Getting elections for user: {}", userId);

        List<Election> elections = electionService.getElectionsForUser(
                UUID.fromString(userId)
        );

        return ResponseEntity.ok(elections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Election> getElectionById(
            @PathVariable UUID id
    ) {
        log.debug("Getting election: {}", id);

        Election election = electionService.getElectionById(id);

        return ResponseEntity.ok(election);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> updateElection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateElectionRequest request
    ) {
        log.info("Updating election: {}", id);

        Election election = electionService.updateElection(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Election updated successfully", election)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteElection(
            @PathVariable UUID id
    ) {
        log.warn("Deleting election: {}", id);

        electionService.deleteElection(id);

        return ResponseEntity.ok(
                ApiResponse.success("Election deleted successfully")
        );
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> activateElection(
            @PathVariable UUID id
    ) {
        log.info("Activating election: {}", id);

        Election election = electionService.activateElection(id);

        return ResponseEntity.ok(
                ApiResponse.success("Election activated successfully", election)
        );
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> closeElection(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId
    ) {
        log.info("Closing election: {}", id);

        Election election = electionService.closeElection(id, UUID.fromString(userId));

        return ResponseEntity.ok(
                ApiResponse.success("Election closed successfully", election)
        );
    }

    @PostMapping("/{id}/publish-results")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> publishResults(
            @PathVariable UUID id
    ) {
        log.info("Publishing results for election: {}", id);

        electionService.publishResults(id);

        return ResponseEntity.ok(
                ApiResponse.success("Results published successfully")
        );
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse> getResults(
            @PathVariable UUID id
    ) {
        log.debug("Getting results for election: {}", id);

        List<Object[]> results = electionService.getElectionResults(id);

        return ResponseEntity.ok(
                ApiResponse.success("Election results", results)
        );
    }

    @PostMapping("/{id}/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> addCandidate(
            @PathVariable UUID id,
            @Valid @RequestBody AddCandidateRequest request
    ) {
        log.info("Adding candidate to election {}: {}", id, request.getName());

        Candidate candidate = electionService.addCandidate(
                id,
                request.getName(),
                request.getPartyAffiliation(),
                request.getBio(),
                request.getPhotoUrl()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate added successfully", candidate));
    }

    @GetMapping("/{id}/candidates")
    public ResponseEntity<List<Candidate>> getCandidates(
            @PathVariable UUID id
    ) {
        log.debug("Getting candidates for election: {}", id);

        List<Candidate> candidates = electionService.getCandidates(id);

        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/{id}/voters")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> addEligibleVoter(
            @PathVariable UUID id,
            @Valid @RequestBody AddEligibleVoterRequest request,
            @AuthenticationPrincipal String adminId
    ) {
        log.info("Adding eligible voter to election {}: {}", id, request.getUserId());

        electionService.addEligibleVoter(
                id,
                request.getUserId(),
                request.getReason(),
                UUID.fromString(adminId)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Eligible voter added successfully"));
    }

    @DeleteMapping("/{electionId}/voters/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> removeEligibleVoter(
            @PathVariable UUID electionId,
            @PathVariable UUID userId
    ) {
        log.info("Removing eligible voter from election {}: {}", electionId, userId);

        electionService.removeEligibleVoter(electionId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Eligible voter removed successfully")
        );
    }

    @GetMapping("/{id}/voters")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<List<ElectionVoter>> getEligibleVoters(
            @PathVariable UUID id
    ) {
        log.debug("Getting eligible voters for election: {}", id);

        List<ElectionVoter> voters = electionService.getEligibleVoters(id);

        return ResponseEntity.ok(voters);
    }

    @GetMapping("/{electionId}/can-vote")
    public ResponseEntity<ApiResponse> canVote(
            @PathVariable UUID electionId,
            @AuthenticationPrincipal String userId
    ) {
        boolean canVote = electionService.canUserVoteInElection(
                UUID.fromString(userId),
                electionId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Can vote status", canVote)
        );
    }
}