package com.univer.voting.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.enums.ElectionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "elections", indexes = {
        @Index(name = "idx_elections_status", columnList = "status"),
        @Index(name = "idx_elections_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_elections_created_by", columnList = "created_by")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ElectionStatus status = ElectionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ElectionType type;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "results_published", nullable = false)
    @Builder.Default
    private Boolean resultsPublished = false;

    @Column(name = "max_votes_per_user", nullable = false)
    @Builder.Default
    private Integer maxVotesPerUser = 1;

    @Column(name = "allow_vote_change", nullable = false)
    @Builder.Default
    private Boolean allowVoteChange = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Candidate> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ElectionVoter> eligibleVoters = new ArrayList<>();

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == ElectionStatus.ACTIVE
                && !now.isBefore(startDate)
                && !now.isAfter(endDate);
    }


    public boolean hasStarted() {
        return !LocalDateTime.now().isBefore(startDate);
    }

    public boolean hasEnded() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isDraft() {
        return status == ElectionStatus.DRAFT;
    }

    public boolean isClosed() {
        return status == ElectionStatus.CLOSED;
    }


    public boolean isRestricted() {
        return !eligibleVoters.isEmpty();
    }

    public long getTotalVotes() {
        return votes.stream()
                .filter(Vote::getIsValid)
                .count();
    }

    public void addCandidate(Candidate candidate) {
        candidates.add(candidate);
        candidate.setElection(this);
    }

    public void removeCandidate(Candidate candidate) {
        candidates.remove(candidate);
        candidate.setElection(null);
    }

    public boolean canBeActivated() {
        return status == ElectionStatus.DRAFT
                && !candidates.isEmpty()
                && startDate != null
                && endDate != null
                && endDate.isAfter(startDate);
    }

    public boolean canBeClosed() {
        return status == ElectionStatus.ACTIVE && hasEnded();
    }

    @Override
    public String toString() {
        return "Election{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalVotes=" + getTotalVotes() +
                '}';
    }
}
