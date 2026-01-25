package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ActiveCasesSummaryDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ActiveCasesSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/active-cases")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class ActiveCasesSummaryController {

    private final ActiveCasesSummaryService activeCasesSummaryService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ActiveCasesSummaryDto>> getSummary(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID lawyerUuid = user.getUuid();

        ActiveCasesSummaryDto summary = activeCasesSummaryService.getSummary(lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Summary fetched successfully", summary)
        );
    }
}
