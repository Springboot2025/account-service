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
@RequestMapping("/api/client/case-events")
@PreAuthorize("hasRole('Client')")
public class ClientCaseEventController {

    private final CaseEventService caseEventService;

    public ClientCaseEventController(CaseEventService caseEventService) {
        this.caseEventService = caseEventService;
    }

    @GetMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<List<CaseEventDto>>> getCaseEvents(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<CaseEventDto> response = caseEventService.getClientCaseEvents(
                caseUuid,
                user.getUuid()
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
        UUID clientUuid = user.getUuid();

        List<CaseEventDto> response =
                caseEventService.getClientUpcomingEvents(clientUuid, limit);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Upcoming case events fetched successfully", response)
        );
    }
}
