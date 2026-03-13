package com.univer.voting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRequest {

    private UUID id;

    @NotBlank(message = "Candidate name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 100, message = "Party affiliation cannot exceed 100 characters")
    private String partyAffiliation;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String photoUrl;
}
