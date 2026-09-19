package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LetterOfAdviceChangeRequestDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceHistoryItemDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentSaveRequest;
import com.legalpro.accountservice.dto.SendLetterOfAdviceRequest;
import com.legalpro.accountservice.dto.SignatureRequest;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LetterOfAdviceDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The lawyer-side of a Letter of Advice's lifecycle: draft -> lawyer signs ->
 * send to client -> (client signs, see ClientLetterOfAdviceDocumentController).
 * One document per case; ownership scoped to the authenticated lawyer. Keyed
 * by caseUuid rather than quoteUuid because a case outlives its originating
 * quote and accumulates many documents over its life.
 */
@RestController
@RequestMapping("/api/lawyer")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerLetterOfAdviceDocumentController {

    private final LetterOfAdviceDocumentService documentService;

    @GetMapping("/cases/{caseUuid}/letter-of-advice")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> getForCase(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto document = documentService
                .getForLawyerByCase(userDetails.getUuid(), caseUuid)
                .orElse(null);
        String message = document == null ? "No Letter of Advice created yet for this matter" : "Letter of Advice fetched successfully";
        return ResponseEntity.ok(ApiResponse.success(200, message, document));
    }

    @PostMapping("/cases/{caseUuid}/letter-of-advice")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> saveDraft(
            @PathVariable UUID caseUuid,
            @RequestBody LetterOfAdviceDocumentSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto saved = documentService.saveDraft(userDetails.getUuid(), caseUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice draft saved successfully", saved));
    }

    @PostMapping("/letter-of-advice/{documentUuid}/sign")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> sign(
            @PathVariable UUID documentUuid,
            @RequestBody SignatureRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto signed = documentService.lawyerSign(userDetails.getUuid(), documentUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice signed successfully", signed));
    }

    @PostMapping("/letter-of-advice/{documentUuid}/send")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> send(
            @PathVariable UUID documentUuid,
            @RequestBody(required = false) SendLetterOfAdviceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto sent = documentService.sendToClient(userDetails.getUuid(), documentUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice sent to client successfully", sent));
    }

    @DeleteMapping("/letter-of-advice/{documentUuid}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        documentService.deleteForLawyer(userDetails.getUuid(), documentUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice deleted successfully", null));
    }

    @GetMapping("/letter-of-advice/{documentUuid}/change-requests")
    public ResponseEntity<ApiResponse<List<LetterOfAdviceChangeRequestDto>>> getChangeRequests(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Change requests fetched successfully",
                documentService.getChangeRequestsForLawyer(userDetails.getUuid(), documentUuid)));
    }

    @PutMapping("/letter-of-advice/{documentUuid}/change-requests/{requestUuid}/read")
    public ResponseEntity<ApiResponse<LetterOfAdviceChangeRequestDto>> markChangeRequestRead(
            @PathVariable UUID documentUuid,
            @PathVariable UUID requestUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Change request marked as read",
                documentService.markChangeRequestRead(userDetails.getUuid(), documentUuid, requestUuid)));
    }

    @PutMapping("/letter-of-advice/{documentUuid}/change-requests/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllChangeRequestsRead(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        documentService.markAllChangeRequestsRead(userDetails.getUuid(), documentUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "All change requests marked as read", null));
    }

    @GetMapping("/cases/{caseUuid}/letter-of-advice/history")
    public ResponseEntity<ApiResponse<List<LetterOfAdviceHistoryItemDto>>> getHistory(
            @PathVariable UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice history fetched successfully",
                documentService.getHistoryForLawyer(userDetails.getUuid(), caseUuid)));
    }

    @PostMapping("/letter-of-advice/{documentUuid}/supersede")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> supersede(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice replaced -- you can now draft a new one",
                documentService.supersede(userDetails.getUuid(), documentUuid)));
    }
}
