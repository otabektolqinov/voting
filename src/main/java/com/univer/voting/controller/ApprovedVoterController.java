package com.univer.voting.controller;

import com.univer.voting.dto.response.ApiResponse;
import com.univer.voting.service.ApprovedVoterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/approved-voters")
@RequiredArgsConstructor
@Slf4j
public class ApprovedVoterController {

    private final ApprovedVoterService approvedVoterService;

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> importApprovedVoters(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String adminId
    ) {
        try {
            int count = approvedVoterService.importFromCsv(file, UUID.fromString(adminId));
            return ResponseEntity.ok(
                    ApiResponse.success("Successfully imported " + count + " approved voters")
            );
        } catch (Exception e) {
            log.error("Failed to import approved voters", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to import: " + e.getMessage()));
        }
    }
}
