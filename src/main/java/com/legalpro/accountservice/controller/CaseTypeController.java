package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CaseTypeDto;
import com.legalpro.accountservice.service.CaseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/case-types")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class CaseTypeController {

    private final CaseTypeService caseTypeService;

    // --- Create new case type ---
    @PostMapping
    public ResponseEntity<ApiResponse<CaseTypeDto>> createCaseType(@RequestBody CaseTypeDto dto) {
        CaseTypeDto created = caseTypeService.createCaseType(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Case type created successfully", created));
    }

    // --- Get all case types ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseTypeDto>>> getAllCaseTypes() {
        List<CaseTypeDto> types = caseTypeService.getAllCaseTypes();
        return ResponseEntity.ok(ApiResponse.success(200, "Case types fetched successfully", types));
    }

    // --- Get single case type by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseTypeDto>> getCaseTypeById(@PathVariable Long id) {
        CaseTypeDto type = caseTypeService.getCaseTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(200, "Case type fetched successfully", type));
    }
}
