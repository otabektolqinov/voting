package com.univer.voting.repository;

import com.univer.voting.models.ElectionVoter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectionVoterRepository extends JpaRepository<ElectionVoter, UUID> {

    List<ElectionVoter> findByElectionId(UUID electionId);
    List<ElectionVoter> findByUserId(UUID userId);
    Optional<ElectionVoter> findByElectionIdAndUserId(UUID electionId, UUID userId);
    boolean existsByElectionId(UUID electionId);
    long countByElectionId(UUID electionId);
}
