package com.univer.voting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateElectionRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Boolean isPublic;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @Size(min = 2, message = "At least 2 candidates are required")
    private List<CandidateRequest> candidates = new ArrayList<>();

    private List<String> voterIds;

}
