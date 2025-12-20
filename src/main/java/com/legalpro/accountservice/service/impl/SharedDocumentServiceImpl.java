package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ShareDocumentRequestDto;
import com.legalpro.accountservice.dto.SharedDocumentResponseDto;
import com.legalpro.accountservice.entity.SharedDocument;
import com.legalpro.accountservice.repository.*;
import com.legalpro.accountservice.service.SharedDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SharedDocumentServiceImpl implements SharedDocumentService {

    private final SharedDocumentRepository sharedDocumentRepository;
    private final DocumentTemplateCenterRepository documentRepository;
    private final LegalCaseRepository caseRepository;

    @Override
    @Transactional
    public List<SharedDocumentResponseDto> shareDocument(
            UUID lawyerUuid,
            UUID documentUuid,
            ShareDocumentRequestDto request
    ) {

        // 1️⃣ Validate request
        if (request.getRecipients() == null || request.getRecipients().isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }

        // 2️⃣ Validate document ownership
        if (!documentRepository.existsByUuidAndLawyerUuidAndDeletedAtIsNull(documentUuid, lawyerUuid)) {
            throw new AccessDeniedException("You are not allowed to share this document");
        }

        List<SharedDocumentResponseDto> response = new ArrayList<>();

        // 3️⃣ Fail-fast per recipient
        for (ShareDocumentRequestDto.RecipientDto recipient : request.getRecipients()) {

            if (recipient.getClientUuid() == null || recipient.getCaseUuid() == null) {
                throw new IllegalArgumentException("clientUuid and caseUuid are mandatory");
            }

            // 4️⃣ Validate case + lawyer + client relationship
            boolean validCase =
                    caseRepository.existsByUuidAndLawyerUuidAndClientUuid(
                            recipient.getCaseUuid(),
                            lawyerUuid,
                            recipient.getClientUuid()
                    );

            if (!validCase) {
                throw new AccessDeniedException(
                        "Case not found, not owned by lawyer, or client does not belong to case"
                );
            }

            // 5️⃣ Persist share
            SharedDocument sharedDocument = SharedDocument.builder()
                    .uuid(UUID.randomUUID())
                    .lawyerUuid(lawyerUuid)
                    .documentUuid(documentUuid)
                    .clientUuid(recipient.getClientUuid())
                    .caseUuid(recipient.getCaseUuid())
                    .remarks(request.getRemarks())
                    .build();

            SharedDocument saved = sharedDocumentRepository.save(sharedDocument);

            response.add(
                    SharedDocumentResponseDto.builder()
                            .uuid(saved.getUuid())
                            .documentUuid(saved.getDocumentUuid())
                            .lawyerUuid(saved.getLawyerUuid())
                            .clientUuid(saved.getClientUuid())
                            .caseUuid(saved.getCaseUuid())
                            .remarks(saved.getRemarks())
                            .createdAt(saved.getCreatedAt())
                            .build()
            );
        }

        return response;
    }
}
