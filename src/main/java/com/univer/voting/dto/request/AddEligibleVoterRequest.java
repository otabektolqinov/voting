package com.univer.voting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEligibleVoterRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    private String reason;
}
