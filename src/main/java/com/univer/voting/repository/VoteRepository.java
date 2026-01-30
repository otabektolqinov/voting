package com.univer.voting.repository;

import com.univer.voting.models.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByUserIdAndElectionId(UUID userId, UUID electionId);
    Optional<Vote> findByUserIdAndElectionId(UUID userId, UUID electionId);
    List<Vote> findByElectionId(UUID electionId);
    long countByElectionIdAndIsValid(UUID electionId, Boolean isValid);

}
