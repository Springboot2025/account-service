package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.QuoteSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.QuoteSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quotes/summary")
public class QuoteSummaryController {

    private final QuoteSummaryService quoteSummaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<QuoteSummaryDto>> getSummary(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        QuoteSummaryDto summary = quoteSummaryService.getSummary(user.getUuid());
        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Quote summary fetched successfully",
                summary
        ));
    }
}
