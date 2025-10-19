package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.LegalCaseMapper;
import com.legalpro.accountservice.repository.CaseStatusRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalCaseServiceImpl implements LegalCaseService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseStatusRepository caseStatusRepository;
    private final LegalCaseMapper mapper;

    // --- Helper: Generate case number ---
    private String generateCaseNumber(UUID lawyerUuid) {
        String prefix = lawyerUuid.toString().substring(0, 4).toUpperCase();
        long count = legalCaseRepository.countActiveByLawyerUuid(lawyerUuid);
        return prefix + "-" + String.format("%06d", count + 1);
    }

    @Override
    public LegalCaseDto createCase(LegalCaseDto dto, UUID lawyerUuid) {
        String caseNumber = generateCaseNumber(lawyerUuid);

        CaseStatus status = caseStatusRepository.findByName("New")
                .orElseThrow(() -> new IllegalStateException("CaseStatus 'New' not found"));

        LegalCase legalCase = LegalCase.builder()
                .uuid(UUID.randomUUID())
                .caseNumber(caseNumber)
                .lawyerUuid(lawyerUuid)
                .clientUuid(dto.getClientUuid())
                .status(status)
                .listing(dto.getListing())
                .courtDate(dto.getCourtDate())
                .availableTrustFunds(dto.getAvailableTrustFunds() != null ? dto.getAvailableTrustFunds() : BigDecimal.ZERO)
                .followUp(dto.getFollowUp())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        legalCaseRepository.save(legalCase);
        return mapper.toDto(legalCase);
    }

    @Override
    public LegalCaseDto updateCase(UUID caseUuid, LegalCaseDto dto, UUID lawyerUuid) {
        LegalCase existing = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!existing.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only update your own cases");
        }

        if (dto.getListing() != null) existing.setListing(dto.getListing());
        if (dto.getCourtDate() != null) existing.setCourtDate(dto.getCourtDate());
        if (dto.getFollowUp() != null) existing.setFollowUp(dto.getFollowUp());
        if (dto.getAvailableTrustFunds() != null) existing.setAvailableTrustFunds(dto.getAvailableTrustFunds());

        if (dto.getStatusId() != null) {
            CaseStatus status = caseStatusRepository.findById(dto.getStatusId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid status ID"));
            existing.setStatus(status);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        legalCaseRepository.save(existing);
        return mapper.toDto(existing);
    }

    @Override
    public LegalCaseDto getCase(UUID caseUuid, UUID lawyerUuid) {
        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("Access denied");
        }

        return mapper.toDto(legalCase);
    }

    @Override
    public List<LegalCaseDto> getCasesForLawyer(UUID lawyerUuid) {
        return legalCaseRepository.findAllByLawyerUuid(lawyerUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCase(UUID caseUuid, UUID lawyerUuid) {
        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new SecurityException("You can only delete your own cases");
        }

        CaseStatus deletedStatus = caseStatusRepository.findByName("Deleted")
                .orElseThrow(() -> new IllegalStateException("CaseStatus 'Deleted' not found"));

        legalCase.setStatus(deletedStatus);
        legalCase.setDeletedAt(LocalDateTime.now());
        legalCaseRepository.save(legalCase);
    }
}
