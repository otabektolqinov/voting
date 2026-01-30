package com.univer.voting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCandidateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String partyAffiliation;
    private String bio;
    private String photoUrl;
}
