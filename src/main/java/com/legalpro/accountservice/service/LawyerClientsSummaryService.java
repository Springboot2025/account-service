package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerClientsSummaryDto;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LawyerClientsSummaryService {

    private final LegalCaseRepository legalCaseRepository;

    public LawyerClientsSummaryDto getSummary(UUID lawyerUuid) {

        LocalDateTime now = LocalDateTime.now();

        // Current month: 1 to today
        LocalDateTime startCurrent = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endCurrent = now;

        // Previous month: full month
        LocalDateTime startPrev = startCurrent.minusMonths(1);
        LocalDateTime endPrev = startCurrent.minusSeconds(1);

        // -----------------------------
        // 1. TOTAL CONTACTS (unique clients)
        // -----------------------------
        long totalContacts =
                legalCaseRepository.countDistinctClients(lawyerUuid);

        long totalContactsPrev =
                legalCaseRepository.countDistinctClientsByMonth(
                        lawyerUuid, startPrev, endPrev);

        // -----------------------------
        // 2. ACTIVE CASES
        // -----------------------------
        long activeCases =
                legalCaseRepository.countByLawyerUuidAndStatus_Name(lawyerUuid, "Active");

        long activeCasesPrev =
                legalCaseRepository.countCasesByStatusAndMonth(
                        lawyerUuid, "Active", startPrev, endPrev);

        // -----------------------------
        // 3. PENDING CASES
        // -----------------------------
        long pendingCases =
                legalCaseRepository.countByLawyerUuidAndStatus_Name(lawyerUuid, "Pending");

        long pendingCasesPrev =
                legalCaseRepository.countCasesByStatusAndMonth(
                        lawyerUuid, "Pending", startPrev, endPrev);

        // -----------------------------
        // 4. REMINDERS
        // -----------------------------
        long pendingReminders =
                legalCaseRepository.countPendingReminders(lawyerUuid);

        // -----------------------------
        // BUILD RESPONSE DTO
        // -----------------------------
        return LawyerClientsSummaryDto.builder()
                .totalContacts(totalContacts)
                .totalContactsGrowth(calcGrowth(totalContacts, totalContactsPrev))

                .activeCases(activeCases)
                .activeCasesGrowth(calcGrowth(activeCases, activeCasesPrev))

                .pendingCases(pendingCases)
                .pendingCasesGrowth(calcGrowth(pendingCases, pendingCasesPrev))

                .pendingReminders(pendingReminders)
                .build();
    }


    private double calcGrowth(long current, long previous) {
        if (previous <= 0) return 0.0;
        return ((double) (current - previous) / previous) * 100;
    }
}
