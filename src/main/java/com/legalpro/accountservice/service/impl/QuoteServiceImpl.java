package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.enums.QuoteStatus;
import com.legalpro.accountservice.mapper.QuoteMapper;
import com.legalpro.accountservice.repository.QuoteRepository;
import com.legalpro.accountservice.service.ActivityLogService;
import com.legalpro.accountservice.service.LegalCaseService;
import com.legalpro.accountservice.service.ProfileService;
import com.legalpro.accountservice.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteMapper quoteMapper;
    private final LegalCaseService legalCaseService;
    private final ActivityLogService activityLogService;
    private final ProfileService profileService;

    // === Client Actions ===

    @Override
    public QuoteDto createQuoteRequest(UUID clientUuid, UUID lawyerUuid, QuoteDto dto) {
        Quote entity = quoteMapper.toEntity(dto);
        entity.setUuid(UUID.randomUUID());
        entity.setClientUuid(clientUuid);
        entity.setLawyerUuid(lawyerUuid);
        entity.setStatus(QuoteStatus.REQUESTED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Quote saved = quoteRepository.save(entity);
        log.info("✅ Quote request created by client {} for lawyer {}", clientUuid, lawyerUuid);

        activityLogService.logActivity(
                "QUOTE_REQUESTED",
                "Client requested a quote",
                clientUuid,                // actorUuid
                lawyerUuid,                // lawyerUuid
                clientUuid,                // clientUuid
                null,                      // caseUuid (not created yet)
                saved.getUuid(),           // referenceUuid (quote UUID)
                Map.of(                    // metadata (optional, safe, clean)
                        "title", dto.getTitle(),
                        "expectedAmount", dto.getExpectedAmount(),
                        "offenceList", dto.getOffenceList()
                )
        );

        return quoteMapper.toDto(saved);
    }

    @Override
    public List<QuoteDto> getQuotesForClient(UUID clientUuid) {
        return quoteRepository.findByClientUuid(clientUuid)
                .stream()
                .map(quoteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public QuoteDto getQuoteForClient(UUID clientUuid, UUID quoteUuid) {
        Quote entity = quoteRepository.findByUuid(quoteUuid)
                .orElseThrow(() -> new RuntimeException("Quote not found"));
        if (!entity.getClientUuid().equals(clientUuid)) {
            throw new RuntimeException("Access denied: not your quote");
        }
        return quoteMapper.toDto(entity);
    }


    // === Lawyer Actions ===

    @Override
    public List<QuoteDto> getQuotesForLawyer(UUID lawyerUuid) {
        List<Quote> list = quoteRepository.findByLawyerUuid(lawyerUuid);

        // Collect all UUIDs needed
        Set<UUID> uuids = new HashSet<>();
        uuids.add(lawyerUuid);
        list.forEach(q -> uuids.add(q.getClientUuid()));

        // Load all accounts in one DB call
        Map<UUID, Account> accounts = profileService.loadAccounts(uuids);

        return list.stream().map(q -> {
            QuoteDto dto = quoteMapper.toDto(q);

            Account clientAcc = accounts.get(q.getClientUuid());
            dto.setClientProfilePictureUrl(
                    profileService.getProfilePicture(clientAcc)
            );

            Account lawyerAcc = accounts.get(q.getLawyerUuid());
            dto.setLawyerProfilePictureUrl(
                    profileService.getProfilePicture(lawyerAcc)
            );

            return dto;
        }).toList();
    }

    @Override
    public QuoteDto getQuoteForLawyer(UUID lawyerUuid, UUID quoteUuid) {
        Quote entity = quoteRepository.findByUuid(quoteUuid)
                .orElseThrow(() -> new RuntimeException("Quote not found"));
        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your quote");
        }
        return quoteMapper.toDto(entity);
    }

    @Override
    @Transactional
    public QuoteDto updateQuoteStatus(UUID lawyerUuid, UUID quoteUuid, QuoteStatus newStatus, String remarks, QuoteDto dto) {

        Quote entity = quoteRepository.findByUuid(quoteUuid)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your quote");
        }

        // update quoted amount if provided
        if (dto.getQuotedAmount() != null) {
            entity.setQuotedAmount(dto.getQuotedAmount());
        }

        if (dto.getCaseTypeId() != null) {
            entity.setCaseTypeId(dto.getCaseTypeId());
        }

        entity.setStatus(newStatus);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setRemarks(remarks != null ? remarks : "");

        Quote saved = quoteRepository.save(entity);
        log.info("✅ Lawyer {} updated quote {} to status {}", lawyerUuid, quoteUuid, newStatus);

        // 🔔 Activity Log — New Client Accepted
        if (newStatus == QuoteStatus.ACCEPTED) {

            activityLogService.logActivity(
                    "QUOTE_ACCEPTED",
                    "Quote accepted by lawyer",
                    lawyerUuid,                 // actorUuid
                    lawyerUuid,                 // lawyerUuid
                    entity.getClientUuid(),     // clientUuid
                    null,                       // caseUuid (NO CASE YET)
                    entity.getUuid(),           // referenceUuid (quote)
                    null                        // metadata
            );
        }

        return quoteMapper.toDto(saved);
    }

    @Override
    public List<QuoteDto> getQuotesForClient(UUID lawyerUuid, UUID clientUuid) {

        List<Quote> quotes = quoteRepository.findByLawyerUuidAndClientUuid(lawyerUuid, clientUuid);

        return quotes.stream()
                .map(quoteMapper::toDto)
                .collect(Collectors.toList());
    }

    private QuoteDto toDto(Quote q) {
        QuoteDto dto = new QuoteDto();
        dto.setId(q.getId());
        dto.setUuid(q.getUuid());
        dto.setLawyerUuid(q.getLawyerUuid());
        dto.setClientUuid(q.getClientUuid());
        dto.setTitle(q.getTitle());
        dto.setDescription(q.getDescription());
        dto.setExpectedAmount(q.getExpectedAmount());
        dto.setQuotedAmount(q.getQuotedAmount());
        dto.setCurrency(q.getCurrency());
        dto.setStatus(q.getStatus());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setUpdatedAt(q.getUpdatedAt());
        dto.setDeletedAt(q.getDeletedAt());
        return dto;
    }

    @Override
    public List<QuoteDto> getQuotesForClientAndLawyer(UUID clientUuid, UUID lawyerUuid) {

        List<Quote> quotes = quoteRepository.findByClientUuidAndLawyerUuid(clientUuid, lawyerUuid);

        return quotes.stream()
                .map(quoteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuoteDto> getRecentQuotesForLawyer(UUID lawyerUuid, Integer limit) {

        Pageable pageable = (limit != null && limit > 0)
                ? PageRequest.of(0, limit)
                : Pageable.unpaged();

        List<Quote> quotes = quoteRepository.findRecentQuotesForLawyer(lawyerUuid, pageable);

        if (quotes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1️⃣ Collect all relevant UUIDs
        Set<UUID> uuids = new HashSet<>();
        uuids.add(lawyerUuid);
        quotes.forEach(q -> {
            uuids.add(q.getClientUuid());
            uuids.add(q.getLawyerUuid());
        });

        // 2️⃣ Load all accounts in one DB call (optimised)
        Map<UUID, Account> accounts = profileService.loadAccounts(uuids);

        // 3️⃣ Map quotes + add profile picture URLs
        return quotes.stream().map(q -> {
            QuoteDto dto = quoteMapper.toDto(q);

            Account clientAcc = accounts.get(q.getClientUuid());
            dto.setClientProfilePictureUrl(
                    profileService.getProfilePicture(clientAcc)
            );

            Account lawyerAcc = accounts.get(q.getLawyerUuid());
            dto.setLawyerProfilePictureUrl(
                    profileService.getProfilePicture(lawyerAcc)
            );

            return dto;
        }).toList();
    }

}
