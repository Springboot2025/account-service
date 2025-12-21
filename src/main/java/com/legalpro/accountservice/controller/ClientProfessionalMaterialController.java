package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientProfessionalMaterialsResponseDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ProfessionalMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client/professional-materials")
@PreAuthorize("hasRole('Client')")
public class ClientProfessionalMaterialController {

    private final ProfessionalMaterialService professionalMaterialService;

    public ClientProfessionalMaterialController(
            ProfessionalMaterialService professionalMaterialService
    ) {
        this.professionalMaterialService = professionalMaterialService;
    }

    // =========================================================
    // GET Professional Materials by Case (Client)
    // =========================================================
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientProfessionalMaterialsResponseDto>>>
    getProfessionalMaterials(
            @RequestParam UUID caseUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        List<ClientProfessionalMaterialsResponseDto> data =
                professionalMaterialService.getClientProfessionalMaterials(
                        userDetails.getUuid(),
                        caseUuid
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Professional materials fetched successfully",
                        data
                )
        );
    }
}
