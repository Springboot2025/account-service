package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.*;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.DocumentTemplateCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/document-template-center")
@PreAuthorize("hasRole('Lawyer')")
public class DocumentTemplateCenterController {

    private final DocumentTemplateCenterService documentService;

    public DocumentTemplateCenterController(
            DocumentTemplateCenterService documentService
    ) {
        this.documentService = documentService;
    }

    // =========================================================
    // 1️⃣ Fetch system-defined categories
    // =========================================================
    @GetMapping("/categories")
    public ResponseEntity<List<DocumentCategoryDto>> getCategories() {
        return ResponseEntity.ok(documentService.getAllCategories());
    }

    // =========================================================
    // 2️⃣ Fetch subheadings grouped by category (lawyer)
    // =========================================================
    @GetMapping("/subheadings")
    public ResponseEntity<List<CategoryWithSubheadingsDto>> getSubheadingsByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        return ResponseEntity.ok(
                documentService.getSubheadingsGroupedByCategory(lawyerUuid)
        );
    }

    // =========================================================
    // 3️⃣ Create new subheading
    // =========================================================
    @PostMapping("/subheadings")
    public ResponseEntity<LawyerDocumentSubheadingDto> createSubheading(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        LawyerDocumentSubheadingDto dto =
                documentService.createSubheading(lawyerUuid, categoryId, name);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // =========================================================
    // 4️⃣ Upload documents (existing OR new subheading)
    // =========================================================
    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public ResponseEntity<List<DocumentTemplateCenterDto>> uploadDocuments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subheadingId,
            @RequestParam(required = false) String newSubheadingName,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        List<DocumentTemplateCenterDto> response =
                documentService.uploadDocuments(
                        lawyerUuid,
                        categoryId,
                        subheadingId,
                        newSubheadingName,
                        files
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/documents/{documentUuid}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        documentService.deleteDocument(userDetails.getUuid(), documentUuid);
        return ResponseEntity.noContent().build();
    }


}
