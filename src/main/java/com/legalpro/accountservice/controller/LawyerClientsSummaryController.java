package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerClientsSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LawyerClientsSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Lawyer')")
public class LawyerClientsSummaryController {

    private final LawyerClientsSummaryService summaryService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<LawyerClientsSummaryDto>> getClientsSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        LawyerClientsSummaryDto summary = summaryService.getSummary(lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Clients summary fetched successfully", summary)
        );
    }
}
