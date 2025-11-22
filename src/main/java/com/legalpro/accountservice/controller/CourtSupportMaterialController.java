package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.entity.CourtSupportMaterial;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.CourtSupportMaterialService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasRole('Client')")
public class CourtSupportMaterialController {

    private final CourtSupportMaterialService service;
    private final ObjectMapper objectMapper;

    public CourtSupportMaterialController(CourtSupportMaterialService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // --- Upload court support materials ---
    @PostMapping("/support-materials")
    public ResponseEntity<ApiResponse<CourtSupportMaterial>> uploadMaterial(
            @RequestParam("clientUuid") UUID clientUuid,
            @RequestParam(value = "caseUuid", required = false) UUID caseUuid,
            @RequestParam("description") String description, // single JSON string
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {

        // Ownership check
        if (!clientUuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only upload your own materials"));
        }

        // Convert JSON string to Map
        Map<String, Object> parsedDescription = objectMapper.readValue(description, new TypeReference<>() {});

        CourtSupportMaterial saved = service.uploadMaterial(clientUuid, caseUuid, parsedDescription, file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Material uploaded successfully", saved));
    }


    // --- Get all materials for a client ---
    @GetMapping("/{uuid}/support-materials")
    public ResponseEntity<ApiResponse<List<CourtSupportMaterial>>> getMaterials(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only view your own materials"));
        }

        List<CourtSupportMaterial> materials = service.getMaterials(uuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Materials fetched successfully", materials));
    }

    // --- Delete (soft delete) a support material ---
    @DeleteMapping("/support-materials/{materialId}")
    public ResponseEntity<ApiResponse<String>> deleteSupportMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        var optionalMaterial = service.getMaterial(materialId);
        if (optionalMaterial.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Support material not found"));
        }

        var material = optionalMaterial.get();

        // Ownership check
        if (!material.getClientUuid().equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only delete your own materials"));
        }

        // Perform soft delete
        service.softDeleteMaterial(materialId);

        return ResponseEntity.ok(ApiResponse.success(200, "Support material deleted successfully", null));
    }

}
