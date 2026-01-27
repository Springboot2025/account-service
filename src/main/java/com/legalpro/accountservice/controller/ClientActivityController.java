package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ActivityLogDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client/dashboard")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientActivityController {

    private final DashboardService dashboardService;

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getClientRecentActivity(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Integer limit
    ) {
        UUID clientUuid = user.getUuid();

        List<ActivityLogDto> activities =
                dashboardService.getClientRecentActivities(clientUuid, limit);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Client recent activity fetched successfully",
                        activities
                )
        );
    }
}

