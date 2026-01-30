package com.univer.voting.repository;

import com.univer.voting.enums.AuditAction;
import com.univer.voting.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId);
    List<AuditLog> findByElectionIdOrderByTimestampDesc(UUID electionId);
    List<AuditLog> findByActionType(AuditAction actionType);
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<AuditLog> findByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}
