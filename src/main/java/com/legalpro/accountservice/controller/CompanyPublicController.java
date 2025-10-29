package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CompanyDto;
import com.legalpro.accountservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyPublicController {

    private final CompanyService companyService;

    // --- Public: Get Company by UUID ---
    @GetMapping("/{companyUuid}")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompany(@PathVariable UUID companyUuid) {
        CompanyDto company = companyService.getCompany(companyUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Company fetched successfully", company));
    }

    // --- Public: Get All Companies ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAllCompanies() {
        List<CompanyDto> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(ApiResponse.success(200, "Companies fetched successfully", companies));
    }
}
