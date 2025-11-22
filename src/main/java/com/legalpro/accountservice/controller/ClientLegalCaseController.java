package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/client/cases")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientLegalCaseController {

    private final LegalCaseService legalCaseService;

    // --- Create Case (Client Side) ---
    @PostMapping
    public ResponseEntity<ApiResponse<LegalCaseDto>> createCaseForClient(
            @RequestBody LegalCaseDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        LegalCaseDto created = legalCaseService.createCaseForClient(dto, clientUuid);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Case created successfully", created));
    }

    // --- Get All Cases for Client ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesForClient(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesForClient(clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    // --- Get Single Case ---
    @GetMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<LegalCaseDto>> getCase(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();

        try {
            LegalCaseDto legalCase = legalCaseService.getCaseForClient(caseUuid, clientUuid);
            return ResponseEntity.ok(ApiResponse.success(200, "Case fetched successfully", legalCase));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    // --- Get Cases by Status ---
    @GetMapping("/status/{statusName}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByStatus(
            @PathVariable String statusName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByStatusForClient(clientUuid, statusName);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    // --- Get Case Summary (By Status & Type) ---
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaseSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        Map<String, Object> summary = legalCaseService.getCaseSummaryForClient(clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Case summary fetched successfully", summary));
    }

    // --- Get Cases by Type ---
    @GetMapping("/type/{typeName}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByType(
            @PathVariable String typeName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByTypeForClient(clientUuid, typeName);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    // --- Update Case (Client Side) ---
    @PatchMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<LegalCaseDto>> updateCaseForClient(
            @PathVariable UUID caseUuid,
            @RequestBody LegalCaseDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();

        try {
            LegalCaseDto updated = legalCaseService.updateCaseForClient(caseUuid, dto, clientUuid);
            return ResponseEntity.ok(ApiResponse.success(200, "Case updated successfully", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

}
