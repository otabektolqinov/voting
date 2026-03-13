package com.univer.voting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CastVoteRequest {
    @NotNull(message = "Election ID is required")
    private UUID electionId;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;
}
