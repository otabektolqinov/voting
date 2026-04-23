package com.univer.voting.repository;

import com.univer.voting.models.VoteOtp;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface VoteOtpRepository extends JpaRepository<VoteOtp, UUID> {
    Optional<VoteOtp> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);

}
