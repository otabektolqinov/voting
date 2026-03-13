package com.univer.voting.dto.response;

import com.univer.voting.enums.ElectionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionDTO {

    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ElectionStatus status;
    private Boolean isPublic;

    private List<CandidateDTO> candidates;

    private Boolean hasVoted;

    private Boolean resultsPublished;

    private Long totalVotes;

    private LocalDateTime createdAt;
    private String createdBy;
}
