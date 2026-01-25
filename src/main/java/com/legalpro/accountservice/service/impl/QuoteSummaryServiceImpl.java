package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.QuoteSummaryDto;
import com.legalpro.accountservice.enums.QuoteStatus;
import com.legalpro.accountservice.repository.QuoteRepository;
import com.legalpro.accountservice.service.QuoteSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteSummaryServiceImpl implements QuoteSummaryService {

    private final QuoteRepository quoteRepository;

    @Override
    public QuoteSummaryDto getSummary(UUID lawyerUuid) {

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().atTime(LocalTime.MAX);

        long newRequests = quoteRepository.countByLawyerUuidAndStatus(
                lawyerUuid, QuoteStatus.REQUESTED
        );

        long urgent = quoteRepository.countByLawyerUuidAndStatus(
                lawyerUuid, QuoteStatus.PENDING
        );

        long acceptedToday = quoteRepository.countByLawyerUuidAndStatusAndUpdatedAtBetween(
                lawyerUuid,
                QuoteStatus.ACCEPTED,
                startOfToday,
                endOfToday
        );

        long thisMonth = quoteRepository.countByLawyerUuidAndCreatedAtBetween(
                lawyerUuid,
                startOfMonth,
                endOfMonth
        );

        return QuoteSummaryDto.builder()
                .newRequests(newRequests)
                .urgent(urgent)
                .acceptedToday(acceptedToday)
                .thisMonth(thisMonth)
                .build();
    }
}
