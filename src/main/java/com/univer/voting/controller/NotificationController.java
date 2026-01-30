package com.univer.voting.controller;

import com.univer.voting.dto.request.NotificationDto;
import com.univer.voting.dto.request.SystemAnnouncementRequest;
import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.models.Notification;
import com.univer.voting.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Getting unread notifications for user: {}", userId);

        List<Notification> notifications = notificationService
                .getUnreadNotifications(UUID.fromString(userId));

        List<NotificationDto> dtos = notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal String userId
    ) {
        long count = notificationService.getUnreadCount(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getAllNotifications(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Getting notifications for user: {} (page: {}, size: {})",
                userId, page, size);

        Page<Notification> notifications = notificationService
                .getAllNotifications(UUID.fromString(userId), page, size);

        Page<NotificationDto> dtos = notifications.map(this::toDTO);

        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId
    ) {
        Notification notification = notificationService.getNotificationById(id);

        // Security check done in service layer
        return ResponseEntity.ok(toDTO(notification));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Marking notification {} as read", id);

        notificationService.markAsRead(id, UUID.fromString(userId));

        return ResponseEntity.ok(
                ApiResponse.success("Notification marked as read")
        );
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Marking all notifications as read for user: {}", userId);

        notificationService.markAllAsRead(UUID.fromString(userId));

        return ResponseEntity.ok(
                ApiResponse.success("All notifications marked as read")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Deleting notification: {}", id);

        notificationService.deleteNotification(id, UUID.fromString(userId));

        return ResponseEntity.ok(
                ApiResponse.success("Notification deleted")
        );
    }

    @DeleteMapping("/read")
    public ResponseEntity<ApiResponse> deleteAllRead(
            @AuthenticationPrincipal String userId
    ) {
        log.debug("Deleting all read notifications for user: {}", userId);

        notificationService.deleteAllRead(UUID.fromString(userId));

        return ResponseEntity.ok(
                ApiResponse.success("Read notifications deleted")
        );
    }

    @PostMapping("/system-announcement")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> sendSystemAnnouncement(
            @RequestBody SystemAnnouncementRequest request
    ) {
        log.info("Sending system announcement: {}", request.getTitle());

        notificationService.sendSystemAnnouncement(
                request.getTitle(),
                request.getMessage(),
                request.getActionUrl()
        );

        return ResponseEntity.ok(
                ApiResponse.success("System announcement sent to all users")
        );
    }

    private NotificationDto toDTO(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType().name());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.getRead());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        dto.setActionUrl(notification.getActionUrl());

        if (notification.getRelatedElection() != null) {
            dto.setElectionId(notification.getRelatedElection().getId());
            dto.setElectionTitle(notification.getRelatedElection().getTitle());
        }

        return dto;
    }
}

