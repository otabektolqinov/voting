package com.univer.voting.service;

import com.univer.voting.models.ApprovedVoter;
import com.univer.voting.repository.ApprovedVoterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovedVoterService {

    private final ApprovedVoterRepository approvedVoterRepository;

    @Transactional
    public int importFromCsv(MultipartFile file, UUID importedBy) throws IOException {
        int count = 0;
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : parser) {
                String nationalId = record.get("nationalId");
                String email = record.get("email");
                String fullName = record.get("fullName");

                // Skip if already exists
                if (approvedVoterRepository.existsByNationalId(nationalId)) {
                    log.warn("Approved voter already exists with nationalId: {}", nationalId);
                    continue;
                }

                if (approvedVoterRepository.existsByEmail(email)) {
                    log.warn("Approved voter already exists with email: {}", email);
                    continue;
                }

                ApprovedVoter approvedVoter = ApprovedVoter.builder()
                        .nationalId(nationalId)
                        .email(email)
                        .fullName(fullName)
                        .importedBy(importedBy)
                        .build();

                approvedVoterRepository.save(approvedVoter);
                count++;
            }
        }
        log.info("Imported {} approved voters", count);
        return count;
    }

    public boolean isApproved(String nationalId, String email) {
        return approvedVoterRepository.existsByNationalIdAndEmail(nationalId, email);
    }
}
