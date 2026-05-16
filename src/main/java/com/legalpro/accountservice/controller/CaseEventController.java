package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.CaseEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/case-events")
@PreAuthorize("hasRole('Lawyer')")
public class CaseEventController {

    private final CaseEventService caseEventService;
    private final LegalCaseRepository legalCaseRepository;
    private final AccountService accountService;

    public CaseEventController(CaseEventService caseEventService,
                               LegalCaseRepository legalCaseRepository,
                               AccountService accountService) {
        this.caseEventService = caseEventService;
        this.legalCaseRepository = legalCaseRepository;
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CaseEventDto>> createCaseEvent(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CaseEventDto dto
    ) {
        CaseEventDto response = caseEventService.createCaseEvent(
                dto.getCaseUuid(),
                user.getUuid(),
                user.getUsername(),
                dto
        );

        return ResponseEntity.ok(
                ApiResponse.success(200, "Case event created successfully", response)
        );
    }

    @GetMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<List<CaseEventDto>>> getCaseEvents(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID lawyerUuid = user.getUuid();

        Account account = accountService.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isCompany = account.isCompany();

        // Company accessing lawyer case
        if (isCompany) {
            LegalCase legalCaseEntity = legalCaseRepository.findByUuid(caseUuid)
                    .orElseThrow(() -> new RuntimeException("Case not found"));

            lawyerUuid = legalCaseEntity.getLawyerUuid();
        }

        List<CaseEventDto> response = caseEventService.getCaseEvents(
                caseUuid,
                lawyerUuid
        );

        return ResponseEntity.ok(
                ApiResponse.success(200, "Case events fetched successfully", response)
        );
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<CaseEventDto>>> getUpcomingEvents(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Integer limit
    ) {
        UUID lawyerUuid = user.getUuid();

        List<CaseEventDto> response =
                caseEventService.getUpcomingEvents(lawyerUuid, limit);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Upcoming events fetched successfully", response)
        );
    }

    @PutMapping("/{eventUuid}")
    public ResponseEntity<ApiResponse<CaseEventDto>> updateCaseEvent(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID eventUuid,
            @RequestBody CaseEventDto dto
    ) {
        CaseEventDto updated = caseEventService.updateCaseEvent(
                eventUuid,
                user.getUuid(),
                dto
        );

        return ResponseEntity.ok(
                ApiResponse.success(200, "Case event updated successfully", updated)
        );
    }

    @DeleteMapping("/{eventUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteCaseEvent(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID eventUuid
    ) {
        caseEventService.deleteCaseEvent(eventUuid, user.getUuid());

        return ResponseEntity.ok(
                ApiResponse.success(200, "Case event deleted successfully", null)
        );
    }
}
