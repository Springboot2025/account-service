package com.legalpro.accountservice.dto;

public record LawyerDashboardSummaryDto(
        SummaryCardDto activeCases,
        SummaryCardDto newRequests,
        SummaryCardDto totalClients,
        WinRateCardDto winRate
) {}
