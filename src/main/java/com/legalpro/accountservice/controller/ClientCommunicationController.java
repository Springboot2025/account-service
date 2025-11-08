package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientCommunicationSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/client/communications")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientCommunicationController {

    private final CommunicationService communicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientCommunicationSummaryDto>>> getCommunications(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();

        log.info("📨 Fetching communications for client: {}", clientUuid);

        List<ClientCommunicationSummaryDto> summaries = communicationService.getClientCommunications(clientUuid, search);

        return ResponseEntity.ok(ApiResponse.success(200, "Communications fetched successfully", summaries));
    }
}
