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
@RequestMapping("/api/lawyer/cases")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LegalCaseController {

    private final LegalCaseService legalCaseService;

    // --- Create Case ---
    @PostMapping
    public ResponseEntity<ApiResponse<LegalCaseDto>> createCase(
            @RequestBody LegalCaseDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        LegalCaseDto created = legalCaseService.createCase(dto, lawyerUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Case created successfully", created));
    }

    // --- Update Case ---
    @PatchMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<LegalCaseDto>> updateCase(
            @PathVariable UUID caseUuid,
            @RequestBody LegalCaseDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        try {
            LegalCaseDto updated = legalCaseService.updateCase(caseUuid, dto, lawyerUuid);
            return ResponseEntity.ok(ApiResponse.success(200, "Case updated successfully", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    // --- Get Single Case ---
    @GetMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<LegalCaseDto>> getCase(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        try {
            LegalCaseDto legalCase = legalCaseService.getCase(caseUuid, lawyerUuid);
            return ResponseEntity.ok(ApiResponse.success(200, "Case fetched successfully", legalCase));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    // --- Get All Cases for Lawyer ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesForLawyer(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    // --- Delete Case ---
    @DeleteMapping("/{caseUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteCase(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        try {
            legalCaseService.deleteCase(caseUuid, lawyerUuid);
            return ResponseEntity.ok(ApiResponse.success(200, "Case deleted successfully", null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    // --- NEW: Get Cases by Status ---
    @GetMapping("/status/{statusName}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByStatus(
            @PathVariable String statusName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByStatus(lawyerUuid, statusName);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    // --- NEW: Get Case Summary by Status ---
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaseSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        Map<String, Object> summary = legalCaseService.getCaseSummary(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Case summary fetched successfully", summary));
    }


    // inside LegalCaseController.java

    // --- Get Cases by Type ---
    @GetMapping("/type/{typeName}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByType(
            @PathVariable String typeName,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByType(lawyerUuid, typeName);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByPriority(
            @PathVariable int priority,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByCasePriority(lawyerUuid, priority);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }

    @GetMapping("/final-status/{finalStatus}")
    public ResponseEntity<ApiResponse<List<LegalCaseDto>>> getCasesByFinalStatus(
            @PathVariable int finalStatus,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<LegalCaseDto> cases = legalCaseService.getCasesByCaseFinalStatus(lawyerUuid, finalStatus);
        return ResponseEntity.ok(ApiResponse.success(200, "Cases fetched successfully", cases));
    }
}
