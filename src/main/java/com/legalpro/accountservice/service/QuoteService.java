package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.enums.QuoteStatus;

import java.util.List;
import java.util.UUID;

public interface QuoteService {

    // === Client Actions ===

    /**
     * Client creates a new quote request for a lawyer.
     */
    QuoteDto createQuoteRequest(UUID clientUuid, UUID lawyerUuid, QuoteDto dto);

    /**
     * Client views all their quote requests.
     */
    List<QuoteDto> getQuotesForClient(UUID clientUuid);

    /**
     * Get specific quote by UUID for a client.
     */
    QuoteDto getQuoteForClient(UUID clientUuid, UUID quoteUuid);


    // === Lawyer Actions ===

    /**
     * Lawyer views all quotes sent to them.
     */
    List<QuoteDto> getQuotesForLawyer(UUID lawyerUuid);

    /**
     * Get specific quote by UUID for a lawyer.
     */
    QuoteDto getQuoteForLawyer(UUID lawyerUuid, UUID quoteUuid);

    /**
     * Lawyer updates quote status or quoted amount.
     */
    QuoteDto updateQuoteStatus(UUID lawyerUuid, UUID quoteUuid, QuoteStatus newStatus, String remarks, QuoteDto dto);
}
