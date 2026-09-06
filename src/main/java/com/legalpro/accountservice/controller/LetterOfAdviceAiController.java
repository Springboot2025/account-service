package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LetterOfAdviceContentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceGenerateRequest;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LetterOfAdviceAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/cases/{caseUuid}/letter-of-advice")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LetterOfAdviceAiController {

    private final LetterOfAdviceAiService letterOfAdviceAiService;

    /**
     * Called when the lawyer clicks "Generate Advice" on the Letter of Advice AI tab.
     * Loads the case's offence details and the client's Core/Offence questionnaire
     * answers server-side, calls Claude, and returns the structured letter content
     * for the frontend to render into the letter builder.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<LetterOfAdviceContentDto>> generate(
            @PathVariable UUID caseUuid,
            @RequestBody LetterOfAdviceGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        LetterOfAdviceContentDto content = letterOfAdviceAiService.generate(lawyerUuid, caseUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Letter of advice drafted successfully", content));
    }
}
