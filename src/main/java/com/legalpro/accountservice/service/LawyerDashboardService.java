package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerDashboardSummaryDto;
import com.legalpro.accountservice.dto.SummaryCardDto;
import com.legalpro.accountservice.dto.WinRateCardDto;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LawyerDashboardService {

    private final LegalCaseRepository legalCaseRepository;
    private final QuoteRepository quoteRepository;
    private final AccountRepository accountRepository;

    public LawyerDashboardSummaryDto getDashboardSummary(UUID lawyerUuid) {

        /* =============================
           DATE WINDOWS
        ============================= */

        LocalDateTime now = LocalDateTime.now();

        // MONTHLY RANGE
        LocalDate firstDayThisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime thisMonthStart = firstDayThisMonth.atStartOfDay();
        LocalDateTime nextMonthStart = thisMonthStart.plusMonths(1);
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

        // WEEKLY RANGE (Monday start)
        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.with(DayOfWeek.MONDAY);

        LocalDateTime thisWeekStart = thisWeekMonday.atStartOfDay();
        LocalDateTime nextWeekStart = thisWeekStart.plusWeeks(1);
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);

        /* =============================
           ACTIVE CASES
        ============================= */

        // Correct: total active cases RIGHT NOW (not based on month)
        int activeTotal = legalCaseRepository
                .findAllByLawyerUuidAndStatusNameIgnoreCase(lawyerUuid, "Active")
                .size();

        // For growth: compare this month vs last month
        int activeThisMonth = legalCaseRepository.countActiveCasesForPeriod(
                lawyerUuid, thisMonthStart, nextMonthStart);

        int activeLastMonth = legalCaseRepository.countActiveCasesForPeriod(
                lawyerUuid, lastMonthStart, thisMonthStart);

        int activeGrowth = calculateGrowth(activeThisMonth, activeLastMonth);

        SummaryCardDto activeCard = new SummaryCardDto(
                activeTotal,
                activeGrowth,
                "from last month"
        );

        /* =============================
           NEW REQUESTS (QUOTES)
        ============================= */

        int newRequestsThisWeek = quoteRepository.countNewRequestsForPeriod(
                lawyerUuid, thisWeekStart, nextWeekStart);

        int newRequestsLastWeek = quoteRepository.countNewRequestsForPeriod(
                lawyerUuid, lastWeekStart, thisWeekStart);

        int newRequestsGrowth = calculateGrowth(newRequestsThisWeek, newRequestsLastWeek);

        SummaryCardDto requestCard = new SummaryCardDto(
                newRequestsThisWeek,
                newRequestsGrowth,
                "this week"
        );

        /* =============================
           TOTAL CLIENTS
        ============================= */

        int totalClients = accountRepository.countTotalClientsForLawyer(lawyerUuid);

        int newClientsThisMonth = accountRepository.countNewClientsForLawyerInPeriod(
                lawyerUuid, thisMonthStart, nextMonthStart);

        int newClientsLastMonth = accountRepository.countNewClientsForLawyerInPeriod(
                lawyerUuid, lastMonthStart, thisMonthStart);

        int clientGrowth = calculateGrowth(newClientsThisMonth, newClientsLastMonth);

        SummaryCardDto clientCard = new SummaryCardDto(
                totalClients,
                clientGrowth,
                "this month"
        );

        /* =============================
           WIN RATE (Not implemented yet)
        ============================= */

        WinRateCardDto winCard = new WinRateCardDto(
                null,
                null,
                "this year"
        );

        /* =============================
           FINAL DTO
        ============================= */

        return new LawyerDashboardSummaryDto(
                activeCard,
                requestCard,
                clientCard,
                winCard
        );
    }

    /* ===================================
       Growth Calculator (No Negative)
    =================================== */
    private int calculateGrowth(int current, int previous) {

        // No negative values ever shown
        if (current <= previous) {
            return 0;
        }

        // If previous = 0 and current > 0 → 100%
        if (previous == 0) {
            return 100;
        }

        double growth = ((double) (current - previous) / previous) * 100;

        return Math.max((int) growth, 0);
    }
}
