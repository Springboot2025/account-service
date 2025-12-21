package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.DocumentShareRecipientDto;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.DocumentShareRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentShareRecipientServiceImpl implements DocumentShareRecipientService {

    private final LegalCaseRepository legalCaseRepository;

    @Override
    public List<DocumentShareRecipientDto> getRecipientsForLawyer(UUID lawyerUuid) {

        // 1️⃣ Fetch all active cases for lawyer
        List<LegalCase> cases =
                legalCaseRepository.findAllForLawyerWithCaseType(lawyerUuid);

        // 2️⃣ Group cases by client
        Map<UUID, List<LegalCase>> casesByClient =
                cases.stream()
                        .collect(Collectors.groupingBy(LegalCase::getClientUuid));

        // 3️⃣ Build response
        List<DocumentShareRecipientDto> response = new ArrayList<>();

        for (Map.Entry<UUID, List<LegalCase>> entry : casesByClient.entrySet()) {

            UUID clientUuid = entry.getKey();
            List<LegalCase> clientCases = entry.getValue();

            // Pick any case to extract client name (if present)
            LegalCase sample = clientCases.get(0);

            List<DocumentShareRecipientDto.CaseDto> caseDtos =
                    clientCases.stream()
                            .map(c -> DocumentShareRecipientDto.CaseDto.builder()
                                    .id(c.getId())
                                    .uuid(c.getUuid())
                                    .caseNumber(c.getCaseNumber())
                                    .listing(c.getListing())
                                    .name(c.getName())
                                    .caseTypeName(
                                            c.getCaseType() != null
                                                    ? c.getCaseType().getName()
                                                    : null
                                    )
                                    .build())
                            .toList();

            response.add(
                    DocumentShareRecipientDto.builder()
                            .clientUuid(clientUuid)
                            .contactName(sample.getName())   // client name if stored
                            .contactInfo(null)               // email comes from user-service later
                            .cases(caseDtos)
                            .build()
            );
        }

        return response;
    }
}
