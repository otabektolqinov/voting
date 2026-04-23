package com.univer.voting.service;

import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.models.Candidate;
import com.univer.voting.models.Election;
import com.univer.voting.models.Users;
import com.univer.voting.models.Vote;
import com.univer.voting.repository.CandidateRepository;
import com.univer.voting.repository.ElectionRepository;
import com.univer.voting.repository.UserRepository;
import com.univer.voting.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final ElectionService electionService;
    private final OtpService otpService;

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";

    @Transactional
    public Vote castVote(UUID userId, UUID electionId, UUID candidateId, String ipAddress, String userAgent, String otp) {
        log.info("Processing encrypted vote: user={}, election={}, candidate={}", userId, electionId, candidateId);

        otpService.validateOtp(userId, otp);

        // 1. Get user
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Get election
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new IllegalArgumentException("Election not found"));

        // 3. Get candidate
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));

        // 4. Validate candidate belongs to election
        if (!candidate.getElection().getId().equals(electionId)) {
            throw new IllegalArgumentException("Candidate does not belong to this election");
        }

        // 5. Check if election is active
        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new IllegalStateException("Election is not active");
        }

        // 6. Check if election time is valid
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(election.getStartDate())) {
            throw new IllegalStateException("Election has not started yet");
        }
        if (now.isAfter(election.getEndDate())) {
            throw new IllegalStateException("Election has ended");
        }

        // 7. Check if user is eligible to vote in this election
        boolean canVote = electionService.canUserVoteInElection(userId, electionId);
        if (!canVote) {
            throw new IllegalStateException("You are not eligible to vote in this election");
        }

        // 8. Check if user has already voted
        if (hasUserVoted(userId, electionId)) {
            throw new IllegalStateException("You have already voted in this election");
        }

        try {
            candidate.setVoteCount(candidate.getVoteCount() + 1);
            candidateRepository.save(candidate);

            // 9. Generate encryption key
            UUID encryptionKeyId = UUID.randomUUID();
            SecretKey secretKey = generateEncryptionKey();

            // 10. Encrypt the vote (candidate ID)
            String encryptedVote = encryptVote(candidateId.toString(), secretKey);

            // 11. Generate vote hash for integrity verification
            String voteHash = generateVoteHash(userId, electionId, candidateId, now);

            // 12. Hash IP address and user agent for privacy
            String ipAddressHash = hashString(ipAddress != null ? ipAddress : "unknown");
            String userAgentHash = hashString(userAgent != null ? userAgent : "unknown");

            // 13. Create and save the vote
            Vote vote = Vote.builder()
                    .election(election)
                    .candidate(candidate)
                    .user(user)
                    .encryptedVote(encryptedVote)
                    .encryptionKeyId(encryptionKeyId)
                    .voteHash(voteHash)
                    .castAt(now)
                    .ipAddressHash(ipAddressHash)
                    .userAgentHash(userAgentHash)
                    .isValid(true)
                    .build();

            vote = voteRepository.save(vote);

            log.info("Encrypted vote successfully cast: voteId={}, hash={}", vote.getId(), voteHash);
            return vote;

        } catch (Exception e) {
            log.error("Error casting encrypted vote", e);
            throw new RuntimeException("Failed to cast vote", e);
        }
    }

    /**
     * Check if a user has voted in an election
     */
    public boolean hasUserVoted(UUID userId, UUID electionId) {
        return voteRepository.existsByUserIdAndElectionId(userId, electionId);
    }

    /**
     * Get vote count for a specific candidate
     */
    public long getVoteCountForCandidate(UUID candidateId) {
        return voteRepository.countByCandidateIdAndIsValidTrue(candidateId);
    }

    /**
     * Get vote count for an election (only valid votes)
     */
    public long getVoteCountForElection(UUID electionId) {
        return voteRepository.countByElectionIdAndIsValidTrue(electionId);
    }

    /**
     * Invalidate a vote (admin function - for fraud detection)
     */
    @Transactional
    public void invalidateVote(UUID voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found"));

        vote.setIsValid(false);
        voteRepository.save(vote);

        log.warn("Vote invalidated: voteId={}", voteId);
    }

    /**
     * Verify vote integrity using hash
     */
    public boolean verifyVoteIntegrity(UUID voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found"));

        try {
            String recalculatedHash = generateVoteHash(
                    vote.getUser().getId(),
                    vote.getElection().getId(),
                    vote.getCandidate().getId(),
                    vote.getCastAt()
            );

            return vote.getVoteHash().equals(recalculatedHash);
        } catch (Exception e) {
            log.error("Error verifying vote integrity", e);
            return false;
        }
    }

    // ==================== Encryption Helper Methods ====================

    /**
     * Generate a new AES encryption key
     */
    private SecretKey generateEncryptionKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGenerator.init(256, new SecureRandom());
        return keyGenerator.generateKey();
    }

    /**
     * Encrypt vote data
     */
    private String encryptVote(String voteData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(voteData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypt vote data (for admin verification only)
     */
    public String decryptVote(String encryptedVote, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedVote));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Generate hash for vote integrity verification
     */
    private String generateVoteHash(UUID userId, UUID electionId, UUID candidateId, LocalDateTime castAt)
            throws Exception {
        String data = userId.toString() + electionId.toString() + candidateId.toString() + castAt.toString();
        return hashString(data);
    }

    /**
     * Hash a string using SHA-256
     */
    private String hashString(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}