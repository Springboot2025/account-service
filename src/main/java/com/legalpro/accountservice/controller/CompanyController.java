package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.CompanyDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/companies")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // --- Create Company ---
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyDto>> createCompany(
            @RequestBody CompanyDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        CompanyDto created = companyService.createCompany(dto, lawyerUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Company created successfully", created));
    }

    // --- Update Company ---
    @PutMapping("/{companyUuid}")
    public ResponseEntity<ApiResponse<CompanyDto>> updateCompany(
            @PathVariable UUID companyUuid,
            @RequestBody CompanyDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        CompanyDto updated = companyService.updateCompany(companyUuid, dto, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Company updated successfully", updated));
    }

    // --- Get Company by UUID ---
    @GetMapping("/{companyUuid}")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompany(@PathVariable UUID companyUuid) {
        CompanyDto company = companyService.getCompany(companyUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Company fetched successfully", company));
    }

    // --- Get All Companies ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAllCompanies() {
        List<CompanyDto> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(ApiResponse.success(200, "Companies fetched successfully", companies));
    }

    // --- Soft Delete Company ---
    @DeleteMapping("/{companyUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        companyService.deleteCompany(companyUuid, lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Company deleted successfully", null));
    }
}
