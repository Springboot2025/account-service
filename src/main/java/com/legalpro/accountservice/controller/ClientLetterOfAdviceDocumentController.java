package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.SignatureRequest;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LetterOfAdviceDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The client-side of a Letter of Advice's lifecycle: view a letter the lawyer
 * has sent, and sign it. Ownership scoped to the authenticated client.
 */
@RestController
@RequestMapping("/api/client/letter-of-advice")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientLetterOfAdviceDocumentController {

    private final LetterOfAdviceDocumentService documentService;

    @GetMapping("/{documentUuid}")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> get(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto document = documentService.getForClient(userDetails.getUuid(), documentUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice fetched successfully", document));
    }

    @PostMapping("/{documentUuid}/sign")
    public ResponseEntity<ApiResponse<LetterOfAdviceDocumentDto>> sign(
            @PathVariable UUID documentUuid,
            @RequestBody SignatureRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceDocumentDto signed = documentService.clientSign(userDetails.getUuid(), documentUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of Advice signed successfully", signed));
    }
}
