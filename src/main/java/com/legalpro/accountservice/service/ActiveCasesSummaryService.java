package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ActiveCasesSummaryDto;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActiveCasesSummaryService {

    private final LegalCaseRepository legalCaseRepository;

    public ActiveCasesSummaryDto getSummary(UUID lawyerUuid) {

        // 1. Total Active Cases (ONLY status = "Active")
        long totalCases =
                legalCaseRepository.countByLawyerUuidAndStatus_Name(lawyerUuid, "Active");

        // 2. Urgent Cases (ONLY status = "Urgent")
        long urgentCases =
                legalCaseRepository.countByLawyerUuidAndStatus_Name(lawyerUuid, "Urgent");

        // 3. Hearings this week (courtDate between Mon–Sun)
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        long hearingsThisWeek = legalCaseRepository
                .findByLawyerUuidAndCourtDateBetween(lawyerUuid, weekStart, weekEnd)
                .size();

        // 4. Won this month (currently: Closed cases)
        long wonThisMonth =
                legalCaseRepository.countByLawyerUuidAndStatus_Name(lawyerUuid, "Closed");

        return ActiveCasesSummaryDto.builder()
                .totalCases(totalCases)
                .urgent(urgentCases)
                .hearingsThisWeek(hearingsThisWeek)
                .wonThisMonth(wonThisMonth)
                .build();
    }
}
