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
    public ResponseEntity<ApiResponse<List<CourtSupportMaterial>>> uploadMaterials(
            @RequestParam("clientUuid") UUID clientUuid,
            @RequestParam("descriptions") List<String> descriptions, // each description is a JSON string
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {

        // Ownership check
        if (!clientUuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only upload your own materials"));
        }

        if (files.size() != descriptions.size()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Each file must have a description JSON"));
        }

        // Convert JSON strings to Map<String,Object>
        List<Map<String, Object>> parsedDescriptions = new ArrayList<>();
        for (String desc : descriptions) {
            Map<String, Object> jsonMap = objectMapper.readValue(desc, new TypeReference<>() {});
            parsedDescriptions.add(jsonMap);
        }

        List<CourtSupportMaterial> saved = service.uploadMaterials(clientUuid, parsedDescriptions, files);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Materials uploaded successfully", saved));
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
}
