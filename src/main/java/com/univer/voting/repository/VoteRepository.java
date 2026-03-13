package com.univer.voting.repository;

import com.univer.voting.models.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    /**
     * Check if a user has voted in a specific election
     */
    boolean existsByUserIdAndElectionId(UUID userId, UUID electionId);

    /**
     * Find a vote by user and election
     */
    Optional<Vote> findByUserIdAndElectionId(UUID userId, UUID electionId);

    /**
     * Count valid votes for a specific candidate
     */
    long countByCandidateIdAndIsValidTrue(UUID candidateId);

    /**
     * Count total valid votes in an election
     */
    long countByElectionIdAndIsValidTrue(UUID electionId);

    /**
     * Get all valid votes for a specific election
     */
    List<Vote> findByElectionIdAndIsValidTrue(UUID electionId);

    /**
     * Get all votes cast by a specific user (for user's voting history)
     */
    List<Vote> findByUserIdAndIsValidTrue(UUID userId);

    /**
     * Find vote by hash (for integrity verification)
     */
    Optional<Vote> findByVoteHash(String voteHash);

    /**
     * Get vote results grouped by candidate for an election (only valid votes)
     */
    @Query("""
        SELECT v.candidate.id as candidateId, 
               v.candidate.name as candidateName,
               v.candidate.partyAffiliation as partyAffiliation,
               COUNT(v) as voteCount
        FROM Vote v
        WHERE v.election.id = :electionId
        AND v.isValid = true
        GROUP BY v.candidate.id, v.candidate.name, v.candidate.partyAffiliation
        ORDER BY COUNT(v) DESC
    """)
    List<Object[]> getElectionResults(@Param("electionId") UUID electionId);

    /**
     * Count invalid votes in an election (for fraud detection)
     */
    long countByElectionIdAndIsValidFalse(UUID electionId);

    /**
     * Find all invalid votes (admin audit)
     */
    List<Vote> findByIsValidFalse();

    /**
     * Get votes by IP address hash (fraud detection)
     */
    List<Vote> findByIpAddressHash(String ipAddressHash);

    /**
     * Count votes from same IP address in an election
     */
    @Query("""
        SELECT COUNT(v)
        FROM Vote v
        WHERE v.election.id = :electionId
        AND v.ipAddressHash = :ipAddressHash
        AND v.isValid = true
    """)
    long countByElectionAndIpAddressHash(
            @Param("electionId") UUID electionId,
            @Param("ipAddressHash") String ipAddressHash
    );

    /**
     * Get voting timeline for an election
     */
    @Query("""
        SELECT DATE(v.castAt) as date, COUNT(v) as count
        FROM Vote v
        WHERE v.election.id = :electionId
        AND v.isValid = true
        GROUP BY DATE(v.castAt)
        ORDER BY DATE(v.castAt)
    """)
    List<Object[]> getVotingTimeline(@Param("electionId") UUID electionId);

    @Query("SELECT v.candidate.id, COUNT(v.id) FROM Vote v " +
            "WHERE v.election.id = :electionId " +
            "GROUP BY v.candidate.id")
    List<Object[]> countVotesByCandidate(@Param("electionId") UUID electionId);

    /**
     * Alternative: Get vote count for ONE specific candidate
     * Useful for real-time updates
     */
    Long countByElectionIdAndCandidateId(UUID electionId, UUID candidateId);

    boolean existsByElectionIdAndUserId(UUID electionId, UUID userId);
}
