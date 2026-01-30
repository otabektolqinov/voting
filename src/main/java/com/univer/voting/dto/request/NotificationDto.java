package com.univer.voting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private UUID id;

    private String type;

    private String title;

    private String message;

    private Boolean read;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    private String actionUrl;

    private UUID electionId;
    private String electionTitle;
}
