package com.univer.voting.repository;

import com.univer.voting.models.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByElectionIdOrderByDisplayOrder(UUID electionId);
    long countByElectionId(UUID electionId);
}
