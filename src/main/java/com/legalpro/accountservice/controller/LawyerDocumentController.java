package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.ClientDocument;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.LetterOfAdviceDocument;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.LetterOfAdviceDocumentRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.ClientDocumentService;
import com.legalpro.accountservice.service.LegalCaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer")
@PreAuthorize("hasRole('Lawyer')")
public class LawyerDocumentController {

    private final ClientDocumentService clientDocumentService;
    private final LegalCaseService legalCaseService;
    private final LegalCaseRepository legalCaseRepository;
    private final AccountService accountService;
    private final LetterOfAdviceDocumentRepository letterOfAdviceDocumentRepository;

    public LawyerDocumentController(ClientDocumentService clientDocumentService,
                                    LegalCaseService legalCaseService,
                                    LegalCaseRepository legalCaseRepository,
                                    AccountService accountService,
                                    LetterOfAdviceDocumentRepository letterOfAdviceDocumentRepository) {
        this.clientDocumentService = clientDocumentService;
        this.legalCaseService = legalCaseService;
        this.legalCaseRepository = legalCaseRepository;
        this.accountService = accountService;
        this.letterOfAdviceDocumentRepository = letterOfAdviceDocumentRepository;
    }

    @GetMapping("/documents/{caseUuid}")
    public ResponseEntity<ApiResponse<List<ClientDocument>>> getClientDocumentsByCase(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        Account account = accountService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isCompany = account.isCompany();

        // Company accessing lawyer case
        if (isCompany) {
            LegalCase legalCaseEntity = legalCaseRepository.findByUuid(caseUuid)
                    .orElseThrow(() -> new RuntimeException("Case not found"));

            lawyerUuid = legalCaseEntity.getLawyerUuid();
        }

        LegalCaseDto legalCaseDto = legalCaseService.getCase(caseUuid, lawyerUuid);
        UUID clientUuid = legalCaseDto.getClientUuid();

        List<ClientDocument> docs = new ArrayList<>(clientDocumentService.getClientDocumentsByCase(clientUuid, caseUuid));

        Optional<LetterOfAdviceDocument> loa = letterOfAdviceDocumentRepository.findByCaseUuidAndDeletedAtIsNull(caseUuid);
        loa.ifPresent(letter -> {
            String frontendBaseUrl = System.getenv("FRONTEND_BASE_URL") != null
                    ? System.getenv("FRONTEND_BASE_URL")
                    : "https://bossjustice.com.au";
            String builderLink = frontendBaseUrl + "/dashboard/communications/letter-of-advice"
                    + "?caseId=" + caseUuid
                    + "&chatId=" + letter.getClientUuid()
                    + "&title=" + (letter.getTitle() != null ? letter.getTitle() : "Letter of Advice");

            docs.add(ClientDocument.builder()
                    .id(-1L)
                    .clientUuid(letter.getClientUuid())
                    .lawyerUuid(letter.getLawyerUuid())
                    .fileName((letter.getTitle() != null ? letter.getTitle() : "Letter of Advice")
                            + " (" + letter.getStatus().name().replace('_', ' ') + ")")
                    .fileType("html")
                    .fileUrl(builderLink)
                    .documentType("Letter_of_Advice")
                    .caseUuid(caseUuid)
                    .createdAt(letter.getUpdatedAt())
                    .build());
        });

        return ResponseEntity.ok(ApiResponse.success(200, "Documents fetched successfully", docs));
    }
}
