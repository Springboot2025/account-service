package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.entity.CaseType;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.LegalCaseMapper;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.CaseStatusRepository;
import com.legalpro.accountservice.repository.CaseTypeRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalCaseServiceImpl implements LegalCaseService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseStatusRepository caseStatusRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final LegalCaseMapper mapper;
    private final AccountRepository accountRepository;

    // --- Helper: Generate case number ---
    private String generateCaseNumber(UUID lawyerUuid) {
        String prefix = lawyerUuid.toString().substring(0, 4).toUpperCase();
        long count = legalCaseRepository.countActiveByLawyerUuid(lawyerUuid);
        return prefix + "-" + String.format("%06d", count + 1);
    }

    // ========================================================
    //                  LAWYER SIDE METHODS
    // ========================================================

    @Override
    public LegalCaseDto createCase(LegalCaseDto dto, UUID lawyerUuid) {
        String caseNumber = generateCaseNumber(lawyerUuid);

        CaseStatus status = caseStatusRepository.findByName("New")
                .orElseThrow(() -> new IllegalStateException("CaseStatus 'New' not found"));

        CaseType caseType = null;
        if (dto.getCaseTypeId() != null) {
            caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid caseTypeId"));
        }

        String clientName = resolveClientName(dto.getClientUuid());

        LegalCase legalCase = LegalCase.builder()
                .uuid(UUID.randomUUID())
                .caseNumber(caseNumber)
                .name(clientName)
                .lawyerUuid(lawyerUuid)
                .clientUuid(dto.getClientUuid())
                .status(status)
                .caseType(caseType)
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

    @Override
    public List<LegalCaseDto> getCasesByStatus(UUID lawyerUuid, String statusName) {
        return legalCaseRepository.findAllByLawyerUuidAndStatusNameIgnoreCase(lawyerUuid, statusName)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCaseSummary(UUID lawyerUuid) {
        List<Object[]> statusCounts = legalCaseRepository.countCasesGroupedByStatus(lawyerUuid);
        Map<String, Long> byStatus = statusCounts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<Object[]> typeCounts = legalCaseRepository.countCasesGroupedByType(lawyerUuid);
        Map<String, Long> byType = typeCounts.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("byStatus", byStatus);
        summary.put("byType", byType);

        return summary;
    }

    @Override
    public List<LegalCaseDto> getCasesByType(UUID lawyerUuid, String typeName) {
        return legalCaseRepository.findAllByLawyerUuidAndCaseType_NameIgnoreCase(lawyerUuid, typeName)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }


    // ========================================================
    //                  CLIENT SIDE METHODS
    // ========================================================

    @Override
    public LegalCaseDto createCaseForClient(LegalCaseDto dto, UUID clientUuid) {

        if (dto.getLawyerUuid() == null) {
            throw new IllegalArgumentException("lawyerUuid is required");
        }

        // ensure lawyer exists
        accountRepository.findByUuid(dto.getLawyerUuid())
                .orElseThrow(() -> new IllegalArgumentException("Invalid lawyerUuid"));

        String caseNumber = generateCaseNumber(dto.getLawyerUuid());

        CaseStatus status = caseStatusRepository.findByName("New")
                .orElseThrow(() -> new IllegalStateException("CaseStatus 'New' not found"));

        CaseType caseType = null;
        if (dto.getCaseTypeId() != null) {
            caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid caseTypeId"));
        }

        String clientName = resolveClientName(clientUuid);

        LegalCase legalCase = LegalCase.builder()
                .uuid(UUID.randomUUID())
                .caseNumber(caseNumber)
                .name(clientName)
                .lawyerUuid(dto.getLawyerUuid())
                .clientUuid(clientUuid)
                .status(status)
                .caseType(caseType)
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
    public List<LegalCaseDto> getCasesForClient(UUID clientUuid) {
        return legalCaseRepository.findAllByClientUuid(clientUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public LegalCaseDto getCaseForClient(UUID caseUuid, UUID clientUuid) {
        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getClientUuid().equals(clientUuid)) {
            throw new SecurityException("You can only access your own cases");
        }

        return mapper.toDto(legalCase);
    }

    @Override
    public List<LegalCaseDto> getCasesByStatusForClient(UUID clientUuid, String statusName) {
        return legalCaseRepository.findAllByLawyerUuidAndStatusNameIgnoreCase(null, statusName)
                .stream()
                .filter(c -> c.getClientUuid().equals(clientUuid))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCaseSummaryForClient(UUID clientUuid) {
        List<LegalCaseDto> cases = getCasesForClient(clientUuid);

        Map<String, Long> byStatus = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatusName(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> byType = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCaseTypeName(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("byStatus", byStatus);
        summary.put("byType", byType);

        return summary;
    }

    @Override
    public List<LegalCaseDto> getCasesByTypeForClient(UUID clientUuid, String typeName) {
        return getCasesForClient(clientUuid)
                .stream()
                .filter(c -> typeName.equalsIgnoreCase(c.getCaseTypeName()))
                .collect(Collectors.toList());
    }

    private String resolveClientName(UUID clientUuid) {
        return accountRepository.findByUuid(clientUuid)
                .map(acc -> {
                    var pd = acc.getPersonalDetails();
                    if (pd == null) return null;
                    if (pd.hasNonNull("fullName")) return pd.get("fullName").asText();
                    if (pd.hasNonNull("name")) return pd.get("name").asText();
                    if (pd.hasNonNull("firstName") && pd.hasNonNull("lastName"))
                        return pd.get("firstName").asText() + " " + pd.get("lastName").asText();
                    return null;
                })
                .orElse(null);
    }
}
