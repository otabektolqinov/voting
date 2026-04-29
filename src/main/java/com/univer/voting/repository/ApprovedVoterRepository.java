package com.univer.voting.repository;

import com.univer.voting.models.ApprovedVoter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovedVoterRepository extends JpaRepository<ApprovedVoter, UUID> {

    boolean existsByNationalIdAndEmail(String nationalId, String email);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
    Optional<ApprovedVoter> findByNationalId(String nationalId);
}
