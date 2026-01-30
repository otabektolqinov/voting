package com.univer.voting.service;

import com.univer.voting.enums.NotificationType;
import com.univer.voting.exception.ForbiddenException;
import com.univer.voting.exception.ResourceNotFoundException;
import com.univer.voting.models.Election;
import com.univer.voting.models.Notification;
import com.univer.voting.models.Users;
import com.univer.voting.repository.ElectionVoterRepository;
import com.univer.voting.repository.NotificationRepository;
import com.univer.voting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Notification Service
 * Handles in-app notifications for users
 *
 * @author Otabek To'lqinov
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ElectionVoterRepository electionVoterRepository;

    @Transactional
    public Notification createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID relatedElectionId,
            String actionUrl
    ) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .relatedElection(relatedElectionId != null ?
                        new Election() {{ setId(relatedElectionId); }} : null)
                .actionUrl(actionUrl)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        log.debug("Created notification for user {}: {}", userId, title);

        return saved;
    }

    /**
     * Notify user about new election
     */
    public void notifyElectionCreated(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.ELECTION_CREATED,
                "New Election Available",
                String.format("A new election '%s' is now available for voting.", election.getTitle()),
                election.getId(),
                "/elections/" + election.getId()
        );
    }

    /**
     * Notify user they've been added to voter list
     */
    public void notifyEligibilityGranted(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.ELIGIBILITY_GRANTED,
                "You Can Vote!",
                String.format("You have been added to the voter list for: %s", election.getTitle()),
                election.getId(),
                "/elections/" + election.getId()
        );
    }

    /**
     * Notify user they've been removed from voter list
     */
    public void notifyEligibilityRevoked(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.ELIGIBILITY_REVOKED,
                "Voting Access Revoked",
                String.format("You have been removed from the voter list for: %s", election.getTitle()),
                election.getId(),
                null
        );
    }

    /**
     * Notify user about election starting soon
     */
    public void notifyElectionStarting(Users user, Election election, long hoursRemaining) {
        createNotification(
                user.getId(),
                NotificationType.ELECTION_STARTING,
                "Election Starting Soon",
                String.format("'%s' starts in %d hours. Get ready to vote!",
                        election.getTitle(), hoursRemaining),
                election.getId(),
                "/elections/" + election.getId()
        );
    }

    /**
     * Notify user about election ending soon
     */
    public void notifyElectionEnding(Users user, Election election, long hoursRemaining) {
        createNotification(
                user.getId(),
                NotificationType.ELECTION_ENDING,
                "Election Ending Soon!",
                String.format("'%s' ends in %d hours. Don't miss your chance to vote!",
                        election.getTitle(), hoursRemaining),
                election.getId(),
                "/elections/" + election.getId()
        );
    }

    /**
     * Notify user about election closed
     */
    public void notifyElectionClosed(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.ELECTION_CLOSED,
                "Election Closed",
                String.format("'%s' has ended. Results will be published soon.",
                        election.getTitle()),
                election.getId(),
                "/elections/" + election.getId()
        );
    }

    /**
     * Notify user about results published
     */
    public void notifyResultsPublished(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.RESULTS_PUBLISHED,
                "Results Published",
                String.format("Results are now available for: %s", election.getTitle()),
                election.getId(),
                "/elections/" + election.getId() + "/results"
        );
    }

    /**
     * Notify user their vote was confirmed
     */
    public void notifyVoteConfirmed(Users user, Election election) {
        createNotification(
                user.getId(),
                NotificationType.VOTE_CONFIRMED,
                "Vote Confirmed",
                String.format("Your vote has been successfully recorded for: %s",
                        election.getTitle()),
                election.getId(),
                "/elections/" + election.getId() + "/confirmation"
        );
    }

    /**
     * Notify user account activated
     */
    public void notifyAccountActivated(Users user) {
        createNotification(
                user.getId(),
                NotificationType.ACCOUNT_ACTIVATED,
                "Account Activated",
                "Your account has been successfully activated. You can now participate in elections.",
                null,
                "/profile"
        );
    }

    /**
     * Send system announcement to all users
     */
    @Transactional
    public void sendSystemAnnouncement(String title, String message, String actionUrl) {
        List<Users> allUsers = userRepository.findAll().stream()
                .filter(Users::getAccountActivated)
                .toList();

        for (Users user : allUsers) {
            createNotification(
                    user.getId(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    title,
                    message,
                    null,
                    actionUrl
            );
        }

        log.info("System announcement sent to {} users", allUsers.size());
    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderBySentAtDesc(userId);
    }

    public Page<Notification> getAllNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
    }

    /**
     * Get notification by ID
     */
    public Notification getNotificationById(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }


    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }


    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = getNotificationById(notificationId);

        // Security check: only owner can mark as read
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot mark other user's notification");
        }

        if (!notification.getRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);

            log.debug("Notification {} marked as read by user {}", notificationId, userId);
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.debug("All notifications marked as read for user {}", userId);
    }


    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = getNotificationById(notificationId);

        // Security check: only owner can delete
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot delete other user's notification");
        }

        notificationRepository.delete(notification);
        log.debug("Notification {} deleted by user {}", notificationId, userId);
    }


    @Transactional
    public void deleteAllRead(UUID userId) {
        List<Notification> readNotifications = notificationRepository
                .findByUserIdOrderBySentAtDesc(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(Notification::getRead)
                .toList();

        notificationRepository.deleteAll(readNotifications);

        log.debug("Deleted {} read notifications for user {}",
                readNotifications.size(), userId);
    }


    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
        notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("Cleaned up notifications older than {}", cutoffDate);
    }


    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void sendElectionEndingReminders() {
        // TODO: Implement logic to find elections ending in 24 hours
        // and notify users who haven't voted yet
        log.debug("Checking for elections ending soon...");
    }
}

