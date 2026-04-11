package com.univer.voting.service.mapper;

import com.univer.voting.dto.response.CandidateDTO;
import com.univer.voting.dto.response.ElectionDTO;
import com.univer.voting.dto.response.VoteDTO;
import com.univer.voting.models.Candidate;
import com.univer.voting.models.Election;
import com.univer.voting.models.Vote;
import com.univer.voting.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper to convert Entities to DTOs
 * This prevents circular reference issues
 */
@Component
@RequiredArgsConstructor
public class ElectionMapper {

    private final VoteRepository voteRepository;

    /**
     * Convert Election entity to DTO
     */
    public ElectionDTO toDTO(Election election) {
        return toDTO(election, null, false);
    }

    /**
     * Convert Election entity to DTO with user-specific data
     */
    public ElectionDTO toDTO(Election election, UUID userId, boolean includeVoteCounts) {
        ElectionDTO dto = ElectionDTO.builder()
                .id(election.getId())
                .title(election.getTitle())
                .description(election.getDescription())
                .startDate(election.getStartDate())
                .endDate(election.getEndDate())
                .status(election.getStatus())
                .resultsPublished(election.getResultsPublished())
//                .isPublic(election.getIsPublic())
                .createdAt(election.getCreatedAt())
//                .createdBy(election.getCreatedBy() != null ? election.getCreatedBy().getFullName() : null)
                .build();

        if (election.getCandidates() != null) {
            dto.setCandidates(election.getCandidates().stream()
                    .map(c -> toCandidateDTO(c, includeVoteCounts))
                    .collect(Collectors.toList()));
        }

        // Check if user has voted (if userId provided)
        if (userId != null) {
            boolean hasVoted = voteRepository.existsByUserIdAndElectionId(userId, election.getId());
            dto.setHasVoted(hasVoted);
        }

        // Include vote counts (for results/admin)
        if (includeVoteCounts) {
            long totalVotes = voteRepository.countByElectionIdAndIsValidTrue(election.getId());
            dto.setTotalVotes(totalVotes);
        }

        return dto;
    }

    /**
     * Convert list of elections to DTOs
     */
    public List<ElectionDTO> toDTOList(List<Election> elections) {
        return elections.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of elections to DTOs with user-specific data
     */
    public List<ElectionDTO> toDTOList(List<Election> elections, UUID userId) {
        return elections.stream()
                .map(e -> toDTO(e, userId, false))
                .collect(Collectors.toList());
    }

    public CandidateDTO toCandidateDTO(Candidate candidate, boolean includeVoteCount) {
        CandidateDTO dto = CandidateDTO.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .partyAffiliation(candidate.getPartyAffiliation())
                .bio(candidate.getBio())
                .build();

        // Include vote count if requested (for results)
        if (includeVoteCount) {
            long voteCount = voteRepository.countByCandidateIdAndIsValidTrue(candidate.getId());
            dto.setVoteCount(voteCount);
        }

        return dto;
    }

    /**
     * Convert Vote entity to DTO
     */
    public VoteDTO toVoteDTO(Vote vote, boolean includeElectionInfo) {
        VoteDTO dto = VoteDTO.builder()
                .id(vote.getId())
                .castAt(vote.getCastAt())
                .voteHash(vote.getVoteHash())
                .build();

        // Include election info if requested (for admin audit)
        if (includeElectionInfo) {
            dto.setElectionId(vote.getElection().getId());
            dto.setElectionTitle(vote.getElection().getTitle());
        }

        return dto;
    }
}
