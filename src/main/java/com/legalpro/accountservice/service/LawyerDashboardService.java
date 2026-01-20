package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerDashboardSummaryDto;
import com.legalpro.accountservice.dto.SummaryCardDto;
import com.legalpro.accountservice.dto.WinRateCardDto;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LawyerDashboardService {

    private final LegalCaseRepository legalCaseRepository;
    private final QuoteRepository quoteRepository;
    private final AccountRepository accountRepository;

    public LawyerDashboardSummaryDto getDashboardSummary(UUID lawyerUuid) {

    /* =====================
       DATE WINDOWS
    ===================== */
        LocalDateTime now = LocalDateTime.now();

        // Monthly ranges
        LocalDateTime thisMonthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime nextMonthStart = thisMonthStart.plusMonths(1);
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

        // Weekly ranges
        LocalDateTime thisWeekStart = now.with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        LocalDateTime nextWeekStart = thisWeekStart.plusWeeks(1);
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);

    /* =====================
       ACTIVE CASES
    ===================== */
        int activeThisMonth = legalCaseRepository.countActiveCasesForPeriod(
                lawyerUuid, thisMonthStart, nextMonthStart);

        int activeLastMonth = legalCaseRepository.countActiveCasesForPeriod(
                lawyerUuid, lastMonthStart, thisMonthStart);

        int activeGrowth = calculateGrowth(activeThisMonth, activeLastMonth);

        SummaryCardDto activeCard = new SummaryCardDto(
                activeThisMonth,
                activeGrowth,
                "from last month"
        );

    /* =====================
       NEW REQUESTS (QUOTES)
    ===================== */
        int newRequestsThisWeek = quoteRepository.countNewRequestsForPeriod(
                lawyerUuid, thisWeekStart, nextWeekStart);

        int newRequestsLastWeek = quoteRepository.countNewRequestsForPeriod(
                lawyerUuid, lastWeekStart, thisWeekStart);

        int newRequestsGrowth = calculateGrowth(newRequestsThisWeek, newRequestsLastWeek);

        SummaryCardDto requestsCard = new SummaryCardDto(
                newRequestsThisWeek,
                newRequestsGrowth,
                "this week"
        );

    /* =====================
       TOTAL CLIENTS
    ===================== */
        int totalClients = accountRepository.countTotalClientsForLawyer(lawyerUuid);

        int newClientsThisMonth = accountRepository.countNewClientsForLawyerInPeriod(
                lawyerUuid, thisMonthStart, nextMonthStart);

        int newClientsLastMonth = accountRepository.countNewClientsForLawyerInPeriod(
                lawyerUuid, lastMonthStart, thisMonthStart);

        int clientGrowth = calculateGrowth(newClientsThisMonth, newClientsLastMonth);

        SummaryCardDto clientsCard = new SummaryCardDto(
                totalClients,
                clientGrowth,
                "this month"
        );

    /* =====================
       WIN RATE (Not implemented)
    ===================== */
        WinRateCardDto winCard = new WinRateCardDto(
                null,
                null,
                "this year"
        );

    /* =====================
       FINAL DTO
    ===================== */
        return new LawyerDashboardSummaryDto(
                activeCard,
                requestsCard,
                clientsCard,
                winCard
        );
    }

    /* ==========================
       Helper: Growth Calculator
    ========================== */
    private int calculateGrowth(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) (((double) (current - previous) / previous) * 100);
    }

}
