package com.univer.voting.repository;

import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.models.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ElectionRepository extends JpaRepository<Election, UUID> {

    List<Election> findByStatus(ElectionStatus status);
    List<Election> findByCreatedBy(UUID createdBy);

    @Query("SELECT e FROM Election e WHERE e.status = 'ACTIVE' " +
            "AND e.startDate <= :now AND e.endDate >= :now")
    List<Election> findActiveElections(LocalDateTime now);

    @Query("SELECT e FROM Election e WHERE e.status = 'ACTIVE' " +
            "AND e.endDate < :now")
    List<Election> findExpiredElections(LocalDateTime now);
}
