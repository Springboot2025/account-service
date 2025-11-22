package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.entity.ClientDocument;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ClientDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasRole('Client')")
public class ClientDocumentController {

    private final ClientDocumentService clientDocumentService;

    public ClientDocumentController(ClientDocumentService clientDocumentService) {
        this.clientDocumentService = clientDocumentService;
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<List<ClientDocument>>> uploadDocuments(
            @RequestParam("clientUuid") UUID clientUuid,
            @RequestParam(value = "lawyerUuid", required = false) UUID lawyerUuid,
            @RequestParam(value = "caseUuid", required = false) UUID caseUuid,
            @RequestParam("documentTypes") List<String> documentTypes,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        // Ownership check
        if (!clientUuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only upload your own documents"));
        }

        if (files.size() != documentTypes.size()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Each file must have a corresponding document type"));
        }

        List<ClientDocument> savedDocuments = clientDocumentService.uploadDocuments(clientUuid, lawyerUuid, caseUuid, documentTypes, files);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Documents uploaded successfully", savedDocuments));
    }



    @GetMapping("/{uuid}/documents")
    public ResponseEntity<ApiResponse<List<ClientDocument>>> getClientDocuments(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only view your own documents"));
        }

        List<ClientDocument> docs = clientDocumentService.getClientDocuments(uuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Documents fetched successfully", docs));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<String>> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Fetch document
        var optionalDoc = clientDocumentService.getDocument(documentId);
        if (optionalDoc.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Document not found"));
        }

        var document = optionalDoc.get();

        // Ownership check
        if (!document.getClientUuid().equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only delete your own documents"));
        }

        // Soft delete
        clientDocumentService.softDeleteDocument(documentId);

        return ResponseEntity.ok(ApiResponse.success(200, "Document deleted successfully", null));
    }

    @GetMapping("/{uuid}/documents/by-type")
    public ResponseEntity<ApiResponse<List<ClientDocument>>> getClientDocumentsByType(
            @PathVariable UUID uuid,
            @RequestParam("documentType") String documentType,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only view your own documents"));
        }

        List<ClientDocument> docs = clientDocumentService.getClientDocumentsByType(uuid, documentType);
        return ResponseEntity.ok(ApiResponse.success(200, "Documents fetched successfully", docs));
    }
}
