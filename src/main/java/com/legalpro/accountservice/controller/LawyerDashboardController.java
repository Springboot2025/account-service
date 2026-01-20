package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerDashboardSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LawyerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/dashboard")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerDashboardController {

    private final LawyerDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<LawyerDashboardSummaryDto>> getSummary(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID lawyerUuid = user.getUuid();

        LawyerDashboardSummaryDto summary =
                dashboardService.getDashboardSummary(lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Dashboard summary fetched successfully",
                        summary
                )
        );
    }
}
