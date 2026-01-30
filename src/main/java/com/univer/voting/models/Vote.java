package com.univer.voting.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_votes_election", columnList = "election_id"),
        @Index(name = "idx_votes_user", columnList = "user_id"),
        @Index(name = "idx_votes_candidate", columnList = "candidate_id"),
        @Index(name = "idx_votes_hash", columnList = "vote_hash")
}, uniqueConstraints = {
        @UniqueConstraint(name = "unique_vote_per_election", columnNames = {"user_id", "election_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "encrypted_vote", nullable = false, columnDefinition = "TEXT")
    private String encryptedVote;

    @Column(name = "encryption_key_id", nullable = false)
    private UUID encryptionKeyId;

    @Column(name = "vote_hash", nullable = false, unique = true, length = 255)
    private String voteHash;

    @Column(name = "cast_at", nullable = false)
    @Builder.Default
    private LocalDateTime castAt = LocalDateTime.now();

    @Column(name = "ip_address_hash", length = 255)
    private String ipAddressHash;

    @Column(name = "user_agent_hash", length = 255)
    private String userAgentHash;

    @Column(name = "is_valid", nullable = false)
    @Builder.Default
    private Boolean isValid = true;
}
