package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientDashboardSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ClientDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/client/dashboard")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientDashboardController {

    private final ClientDashboardService clientDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ClientDashboardSummaryDto>> getClientSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();

        ClientDashboardSummaryDto summary = clientDashboardService.getSummary(clientUuid);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Client dashboard summary fetched successfully",
                        summary
                )
        );
    }
}
