package com.univer.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.univer.voting.models.Session;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshToken(String refreshToken);

    Optional<Session> findByUserIdAndSessionToken(UUID userId, String sessionToken);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = FALSE WHERE s.user.id = :userId")
    void deactivateAllUserSessions(UUID userId);
}
