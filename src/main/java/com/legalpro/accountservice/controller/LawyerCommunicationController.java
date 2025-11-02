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
@RequestMapping("/api/lawyer/communications")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerCommunicationController {

    private final CommunicationService communicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientCommunicationSummaryDto>>> getCommunications(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<ClientCommunicationSummaryDto> summaries = communicationService.getLawyerCommunications(lawyerUuid, search);
        return ResponseEntity.ok(ApiResponse.success(200, "Communications fetched successfully", summaries));
    }
}
