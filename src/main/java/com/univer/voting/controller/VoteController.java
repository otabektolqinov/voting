package com.univer.voting.controller;

import com.univer.voting.dto.request.CastVoteRequest;
import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.models.Vote;
import com.univer.voting.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Slf4j
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    @PreAuthorize("hasRole('VOTER')")
    public ResponseEntity<ApiResponse> castVote(
            @Valid @RequestBody CastVoteRequest request,
            @AuthenticationPrincipal String userId,
            HttpServletRequest httpRequest
    ) {
        log.info("User {} casting vote in election {}", userId, request.getElectionId());
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println(request.getOtp());
        try {
            // Extract IP address and user agent for audit trail (hashed for privacy)
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            Vote vote = voteService.castVote(
                    UUID.fromString(userId),
                    request.getElectionId(),
                    request.getCandidateId(),
                    ipAddress,
                    userAgent,
                    request.getOtp()
            );

            // Return minimal information (don't expose candidate choice)
            Map<String, Object> response = Map.of(
                    "voteId", vote.getId(),
                    "castAt", vote.getCastAt(),
                    "voteHash", vote.getVoteHash()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Vote cast successfully", response));

        } catch (IllegalStateException e) {
            // User already voted, election not active, etc.
            log.warn("Vote rejected for user {}: {}", userId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException e) {
            // Invalid input (election not found, etc.)
            log.warn("Invalid vote request from user {}: {}", userId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error casting vote for user " + userId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to cast vote. Please try again."));
        }
    }


    @GetMapping("/check/{electionId}")
    @PreAuthorize("hasRole('VOTER')")
    public ResponseEntity<ApiResponse> checkIfVoted(
            @PathVariable UUID electionId,
            @AuthenticationPrincipal String userId
    ) {
        boolean hasVoted = voteService.hasUserVoted(
                UUID.fromString(userId),
                electionId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Vote status", hasVoted)
        );
    }

    @GetMapping("/candidate/{candidateId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> getVoteCount(
            @PathVariable UUID candidateId
    ) {
        long count = voteService.getVoteCountForCandidate(candidateId);

        return ResponseEntity.ok(
                ApiResponse.success("Vote count", count)
        );
    }


    @GetMapping("/election/{electionId}/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> getElectionVoteCount(
            @PathVariable UUID electionId
    ) {
        long count = voteService.getVoteCountForElection(electionId);

        return ResponseEntity.ok(
                ApiResponse.success("Total votes", count)
        );
    }

    /**
     * Verify vote integrity (admin only)
     */
    @GetMapping("/verify/{voteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ELECTION_OFFICER')")
    public ResponseEntity<ApiResponse> verifyVoteIntegrity(
            @PathVariable UUID voteId
    ) {
        boolean isValid = voteService.verifyVoteIntegrity(voteId);

        return ResponseEntity.ok(
                ApiResponse.success("Vote integrity", Map.of(
                        "voteId", voteId,
                        "isValid", isValid
                ))
        );
    }

    /**
     * Invalidate a vote (admin only - for fraud detection)
     */
    @PostMapping("/invalidate/{voteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> invalidateVote(
            @PathVariable UUID voteId
    ) {
        log.warn("Admin invalidating vote: {}", voteId);

        voteService.invalidateVote(voteId);

        return ResponseEntity.ok(
                ApiResponse.success("Vote invalidated successfully")
        );
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_VIA");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // If multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}