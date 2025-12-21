package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ProfessionalMaterialResponseDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ProfessionalMaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/professional-materials")
@PreAuthorize("hasRole('Lawyer')")
public class ProfessionalMaterialController {

    private final ProfessionalMaterialService professionalMaterialService;

    public ProfessionalMaterialController(ProfessionalMaterialService professionalMaterialService) {
        this.professionalMaterialService = professionalMaterialService;
    }

    // =========================================================
    // 1️⃣ Upload Professional Material
    // =========================================================
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfessionalMaterialResponseDto>> uploadProfessionalMaterial(
            @RequestParam UUID caseUuid,
            @RequestParam Long documentCatId,
            @RequestParam String followUp,
            @RequestParam String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {

        ProfessionalMaterialResponseDto response =
                professionalMaterialService.uploadProfessionalMaterial(
                        userDetails.getUuid(),
                        caseUuid,
                        documentCatId,
                        followUp,
                        description,
                        file
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "Professional material uploaded successfully",
                                response
                        )
                );
    }
}
