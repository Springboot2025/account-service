package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.enums.QuoteStatus;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.QuoteService;
import lombok.Data;
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
@RequestMapping("/api/lawyer/quotes")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerQuoteController {

    private final QuoteService quoteService;

    // === Get all quotes assigned to this lawyer ===
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuoteDto>>> getLawyerQuotes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<QuoteDto> quotes = quoteService.getQuotesForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Quotes fetched successfully", quotes));
    }

    // === Get a specific quote ===
    @GetMapping("/{quoteUuid}")
    public ResponseEntity<ApiResponse<QuoteDto>> getQuoteById(
            @PathVariable UUID quoteUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        QuoteDto quote = quoteService.getQuoteForLawyer(lawyerUuid, quoteUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Quote details fetched successfully", quote));
    }

    // === Update quote (status, quotedAmount, remarks) ===
    @PutMapping("/{quoteUuid}")
    public ResponseEntity<ApiResponse<QuoteDto>> updateQuote(
            @PathVariable UUID quoteUuid,
            @RequestBody QuoteUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        QuoteDto dto = new QuoteDto();
        dto.setQuotedAmount(request.getQuotedAmount());

        if (request.getCaseTypeId() != null) {
            dto.setCaseTypeId(request.getCaseTypeId());
        }

        QuoteDto updated = quoteService.updateQuoteStatus(
                lawyerUuid,
                quoteUuid,
                request.getStatus(),
                request.getRemarks(),
                dto
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, "Quote updated successfully", updated));
    }

    // Inner DTO just for updates (clean separation)
    @Data
    public static class QuoteUpdateRequest {
        private QuoteStatus status;
        private String remarks;
        private Long caseTypeId;
        private java.math.BigDecimal quotedAmount;
    }

    // === Get all quotes for a specific client (only for this lawyer) ===
    @GetMapping("/client/{clientUuid}")
    public ResponseEntity<ApiResponse<List<QuoteDto>>> getQuotesForClient(
            @PathVariable UUID clientUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        List<QuoteDto> quotes = quoteService.getQuotesForClient(lawyerUuid, clientUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Client quotes fetched successfully", quotes)
        );
    }

}
