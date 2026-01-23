package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ActivityLogDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer/dashboard")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class ActivityLogController {

    private final DashboardService dashboardService;

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getRecentActivity(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Integer limit
    ) {
        UUID lawyerUuid = user.getUuid();

        List<ActivityLogDto> activities =
                dashboardService.getRecentActivities(lawyerUuid, limit);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Recent activity fetched successfully",
                        activities
                )
        );
    }
}
