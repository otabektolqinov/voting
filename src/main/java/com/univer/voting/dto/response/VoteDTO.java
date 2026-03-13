package com.univer.voting.dto.response;

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
public class VoteDTO {
    private UUID id;
    private LocalDateTime castAt;
    private String voteHash;

    private UUID electionId;
    private String electionTitle;
}
