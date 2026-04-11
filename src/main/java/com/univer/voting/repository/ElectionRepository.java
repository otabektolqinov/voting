package com.univer.voting.repository;

import com.univer.voting.enums.ElectionStatus;
import com.univer.voting.models.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    @Query("select e FROM Election e where e.type = 'PUBLIC' " +
            "AND e.status = 'ACTIVE'"
    )
    List<Election> findAllPublicElection();

    @Query("SELECT DISTINCT e FROM Election e " +
            "LEFT JOIN e.eligibleVoters ev " +
            "WHERE (e.type = 'PUBLIC' OR ev.id = :userId) " +
            "AND (e.status = 'ACTIVE' OR (e.status = 'CLOSED' AND e.resultsPublished = true)) " +
            "ORDER BY e.startDate DESC")
    List<Election> findElectionsForVoter(@Param("userId") UUID userId);

    @Modifying
    @Query("""
    UPDATE Election e
    SET e.status = 'CLOSED'
    WHERE e.endDate < :now AND e.status = 'ACTIVE'
""")
    void closeExpiredElections(@Param("now") LocalDateTime now);

}
