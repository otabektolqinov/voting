package com.univer.voting.repository;

import com.univer.voting.models.Notification;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndReadFalseOrderBySentAtDesc(UUID userId);
    Page<Notification> findByUserIdOrderBySentAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = FALSE")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = TRUE, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.user.id = :userId AND n.read = FALSE")
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.read = TRUE AND n.readAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

}
