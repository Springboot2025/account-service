package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.enums.QuoteStatus;
import com.legalpro.accountservice.mapper.QuoteMapper;
import com.legalpro.accountservice.repository.QuoteRepository;
import com.legalpro.accountservice.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteMapper quoteMapper;

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
        return quoteRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .map(quoteMapper::toDto)
                .collect(Collectors.toList());
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
    public QuoteDto updateQuoteStatus(UUID lawyerUuid, UUID quoteUuid, QuoteStatus newStatus, String remarks, QuoteDto dto) {
        Quote entity = quoteRepository.findByUuid(quoteUuid)
                .orElseThrow(() -> new RuntimeException("Quote not found"));

        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your quote");
        }

        // update details (e.g. lawyer providing quoted amount or status change)
        if (dto.getQuotedAmount() != null) {
            entity.setQuotedAmount(dto.getQuotedAmount());
        }
        entity.setStatus(newStatus);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDescription(remarks != null ? remarks : entity.getDescription());

        Quote saved = quoteRepository.save(entity);
        log.info("✅ Lawyer {} updated quote {} to status {}", lawyerUuid, quoteUuid, newStatus);
        return quoteMapper.toDto(saved);
    }

    @Override
    public List<QuoteDto> getQuotesForClient(UUID lawyerUuid, UUID clientUuid) {

        List<Quote> quotes = quoteRepository.findByLawyerUuidAndClientUuid(lawyerUuid, clientUuid);

        return quotes.stream()
                .map(this::toDto)
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
                .map(this::toDto)
                .collect(Collectors.toList());
    }

}
