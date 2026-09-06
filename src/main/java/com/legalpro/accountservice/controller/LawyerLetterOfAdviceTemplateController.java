package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LetterOfAdviceTemplateDto;
import com.legalpro.accountservice.dto.LetterOfAdviceTemplateRequest;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LetterOfAdviceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Letter of Advice template library — ownership is scoped to the authenticated
 * lawyer; a lawyer can only see, edit, and delete their own templates.
 */
@RestController
@RequestMapping("/api/lawyer/letter-templates")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerLetterOfAdviceTemplateController {

    private final LetterOfAdviceTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LetterOfAdviceTemplateDto>>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<LetterOfAdviceTemplateDto> templates = templateService.getTemplatesForLawyer(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(200, "Templates fetched successfully", templates));
    }

    @GetMapping("/{templateUuid}")
    public ResponseEntity<ApiResponse<LetterOfAdviceTemplateDto>> get(
            @PathVariable UUID templateUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceTemplateDto template = templateService.getTemplate(userDetails.getUuid(), templateUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Template fetched successfully", template));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LetterOfAdviceTemplateDto>> create(
            @RequestBody LetterOfAdviceTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceTemplateDto created = templateService.createTemplate(userDetails.getUuid(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Template created successfully", created));
    }

    @PutMapping("/{templateUuid}")
    public ResponseEntity<ApiResponse<LetterOfAdviceTemplateDto>> update(
            @PathVariable UUID templateUuid,
            @RequestBody LetterOfAdviceTemplateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LetterOfAdviceTemplateDto updated = templateService.updateTemplate(userDetails.getUuid(), templateUuid, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Template updated successfully", updated));
    }

    @DeleteMapping("/{templateUuid}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID templateUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        templateService.deleteTemplate(userDetails.getUuid(), templateUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Template deleted successfully", null));
    }
}
