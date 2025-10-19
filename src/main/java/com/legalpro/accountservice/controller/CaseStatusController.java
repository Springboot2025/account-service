package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CaseStatusDto;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.repository.CaseStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/case-status")
@PreAuthorize("hasAnyRole('Lawyer', 'Admin')")
@RequiredArgsConstructor
public class CaseStatusController {

    private final CaseStatusRepository caseStatusRepository;

    // --- Get All Case Statuses ---
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseStatusDto>>> getAllStatuses() {
        List<CaseStatusDto> statuses = caseStatusRepository.findAll().stream()
                .map(status -> CaseStatusDto.builder()
                        .id(status.getId())
                        .name(status.getName())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(200, "Statuses fetched successfully", statuses));
    }

    // --- Get Case Status by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStatusDto>> getStatusById(@PathVariable Long id) {
        Optional<CaseStatus> optionalStatus = caseStatusRepository.findById(id);

        if (optionalStatus.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "Case status not found"));
        }

        CaseStatus status = optionalStatus.get();
        CaseStatusDto dto = CaseStatusDto.builder()
                .id(status.getId())
                .name(status.getName())
                .build();

        return ResponseEntity.ok(ApiResponse.success(200, "Case status fetched successfully", dto));
    }
}
