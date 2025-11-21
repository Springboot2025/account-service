package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.security.CustomUserDetails;
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

    public CaseEventController(CaseEventService caseEventService) {
        this.caseEventService = caseEventService;
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
        List<CaseEventDto> response = caseEventService.getCaseEvents(
                caseUuid,
                user.getUuid()
        );

        return ResponseEntity.ok(
                ApiResponse.success(200, "Case events fetched successfully", response)
        );
    }
}
