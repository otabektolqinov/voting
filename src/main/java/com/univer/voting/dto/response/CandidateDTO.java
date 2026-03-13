package com.univer.voting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {
    private UUID id;
    private String name;
    private String partyAffiliation;
    private String bio;

    private Long voteCount;
}
