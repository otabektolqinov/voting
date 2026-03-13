package com.univer.voting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResultDTO {

    private String candidateId;
    private String candidateName;
    private String partyAffiliation;

    private Long voteCount;
    private Double percentage;

    private Integer rank;
}
