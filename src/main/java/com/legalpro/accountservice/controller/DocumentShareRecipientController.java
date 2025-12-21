package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.DocumentShareRecipientDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.DocumentShareRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/document-share")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Lawyer')")
public class DocumentShareRecipientController {

    private final DocumentShareRecipientService recipientService;

    /**
     * 🔗 Get recipients (clients + their cases) for document sharing
     */
    @GetMapping("/recipients")
    public ResponseEntity<ApiResponse<List<DocumentShareRecipientDto>>> getRecipients(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        List<DocumentShareRecipientDto> recipients =
                recipientService.getRecipientsForLawyer(lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "All contacts fetched successfully",
                        recipients
                )
        );
    }
}
