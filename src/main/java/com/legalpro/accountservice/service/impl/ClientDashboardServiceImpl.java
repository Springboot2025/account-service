package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ClientDashboardSummaryDto;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.ClientDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientDashboardServiceImpl implements ClientDashboardService {

    private final LegalCaseRepository legalCaseRepository;

    @Override
    public ClientDashboardSummaryDto getSummary(UUID clientUuid) {

        List<String> excludedStatuses = List.of("Closed", "Deleted");

        // 1. Active cases
        long activeCases =
                legalCaseRepository.countByClientUuidAndStatus_NameNotInAndDeletedAtIsNull(
                        clientUuid,
                        excludedStatuses
                );

        // 2. New cases this month
        LocalDateTime startOfMonth =
                LocalDate.now().withDayOfMonth(1).atStartOfDay();

        LocalDateTime startOfNextMonth =
                LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        long newCasesThisMonth =
                legalCaseRepository.countByClientUuidAndCreatedAtBetweenAndDeletedAtIsNull(
                        clientUuid,
                        startOfMonth,
                        startOfNextMonth
                );

        // 3. Upcoming hearing dates (next 30 days)
        long upcomingDates =
                legalCaseRepository.countByClientUuidAndCourtDateBetweenAndDeletedAtIsNull(
                        clientUuid,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30)
                );

        return ClientDashboardSummaryDto.builder()
                .activeCases(activeCases)
                .newCasesThisMonth(newCasesThisMonth)
                .upcomingDates(upcomingDates)
                .trustBalance(null)  // placeholder
                .hoursBilled(null)   // placeholder
                .build();
    }
}
