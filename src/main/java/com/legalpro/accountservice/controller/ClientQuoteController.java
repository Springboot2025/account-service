package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/client/quotes")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientQuoteController {

    private final QuoteService quoteService;

    // === Request a new quote ===
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<QuoteDto>> requestQuote(
            @RequestBody QuoteDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        if (dto.getLawyerUuid() == null) {
            throw new IllegalArgumentException("lawyerUuid is required in request body");
        }

        QuoteDto created = quoteService.createQuoteRequest(clientUuid, dto.getLawyerUuid(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Quote request created successfully", created));
    }

    // === Get all quotes for this client ===
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuoteDto>>> getClientQuotes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        List<QuoteDto> quotes = quoteService.getQuotesForClient(clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Client quotes fetched successfully", quotes));
    }

    // === Get specific quote ===
    @GetMapping("/{quoteUuid}")
    public ResponseEntity<ApiResponse<QuoteDto>> getClientQuote(
            @PathVariable UUID quoteUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        QuoteDto quote = quoteService.getQuoteForClient(clientUuid, quoteUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Quote details fetched successfully", quote));
    }
}
