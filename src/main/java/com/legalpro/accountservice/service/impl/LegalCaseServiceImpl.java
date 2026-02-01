package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.LegalCaseDto;
import com.legalpro.accountservice.entity.*;
import com.legalpro.accountservice.enums.QuoteStatus;
import com.legalpro.accountservice.mapper.LegalCaseMapper;
import com.legalpro.accountservice.repository.*;
import com.legalpro.accountservice.service.ActivityLogService;
import com.legalpro.accountservice.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalCaseServiceImpl implements LegalCaseService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseStatusRepository caseStatusRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final LegalCaseMapper mapper;
    private final AccountRepository accountRepository;
    private final QuoteRepository quoteRepository;
    private final ActivityLogService activityLogService;

    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com/legalpro";

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

        String description = clientName + " - " +
                (caseType != null ? caseType.getName() : dto.getListing()) +
                " accepted and added to active cases";

        boolean isFirstCase = !legalCaseRepository
                .existsByClientUuidAndLawyerUuid(dto.getClientUuid(), lawyerUuid);

        if (isFirstCase) {
            activityLogService.logActivity(
                    "NEW_CLIENT_ACCEPTED",
                    description,
                    lawyerUuid,
                    lawyerUuid,
                    dto.getClientUuid(),
                    legalCase.getUuid(),
                    null,
                    null
            );
        }

        activityLogService.logActivity(
                "CASE_CREATED",
                description,
                lawyerUuid,
                lawyerUuid,
                dto.getClientUuid(),
                legalCase.getUuid(),
                null,
                null
        );

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

        if (dto.getCasePriority() != null) existing.setCasePriority(dto.getCasePriority());
        if (dto.getCaseFinalStatus() != null) existing.setCaseFinalStatus(dto.getCaseFinalStatus());

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

        // Map case data (mapper loads Quote title + offenceList)
        LegalCaseDto dto = mapper.toDto(legalCase);

        // Load BOTH accounts at once
        Set<UUID> uuids = Set.of(legalCase.getClientUuid(), legalCase.getLawyerUuid());

        Map<UUID, Account> accounts = accountRepository.findByUuidIn(uuids)
                .stream()
                .collect(Collectors.toMap(Account::getUuid, a -> a));

        // Set client pic
        Account clientAcc = accounts.get(legalCase.getClientUuid());
        if (clientAcc != null) {
            dto.setClientProfilePictureUrl(convertGcsUrl(clientAcc.getProfilePictureUrl()));
        }

        // Set lawyer pic
        Account lawyerAcc = accounts.get(legalCase.getLawyerUuid());
        if (lawyerAcc != null) {
            dto.setLawyerProfilePictureUrl(convertGcsUrl(lawyerAcc.getProfilePictureUrl()));
        }

        return dto;
    }

    @Override
    public List<LegalCaseDto> getCasesForLawyer(UUID lawyerUuid) {
        List<LegalCase> cases = legalCaseRepository.findAllByLawyerUuid(lawyerUuid);

        // Collect all client UUIDs
        Set<UUID> clientUuids = cases.stream()
                .map(LegalCase::getClientUuid)
                .collect(Collectors.toSet());

        // Add lawyer UUID for bulk load
        clientUuids.add(lawyerUuid);

        // Load all accounts in one call
        Map<UUID, Account> accounts = accountRepository.findByUuidIn(clientUuids)
                .stream().collect(Collectors.toMap(Account::getUuid, a -> a));

        return cases.stream().map(c -> {
            LegalCaseDto dto = mapper.toDto(c);

            Account clientAcc = accounts.get(c.getClientUuid());
            if (clientAcc != null) {
                dto.setClientProfilePictureUrl(convertGcsUrl(clientAcc.getProfilePictureUrl()));
            }

            Account lawyerAcc = accounts.get(c.getLawyerUuid());
            if (lawyerAcc != null) {
                dto.setLawyerProfilePictureUrl(convertGcsUrl(lawyerAcc.getProfilePictureUrl()));
            }

            return dto;
        }).collect(Collectors.toList());
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

        if (dto.getQuoteUuid() == null) {
            throw new IllegalArgumentException("quoteUuid is required");
        }

        Quote quote = quoteRepository.findByUuidAndClientUuid(dto.getQuoteUuid(), clientUuid)
                .orElseThrow(() -> new IllegalArgumentException("Invalid quoteUuid for the client"));
        quote.setStatus(QuoteStatus.BOOKED);

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
                .quoteUuid(dto.getQuoteUuid())
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
        quoteRepository.save(quote);
        return mapper.toDto(legalCase);
    }

    @Override
    public List<LegalCaseDto> getCasesForClient(UUID clientUuid) {
        List<LegalCase> cases = legalCaseRepository.findAllByClientUuid(clientUuid);

        // Collect all lawyer UUIDs
        Set<UUID> lawyerUuids = cases.stream()
                .map(LegalCase::getLawyerUuid)
                .collect(Collectors.toSet());

        // Add client UUID for bulk load
        lawyerUuids.add(clientUuid);

        // Load all accounts in one call
        Map<UUID, Account> accounts = accountRepository.findByUuidIn(lawyerUuids)
                .stream().collect(Collectors.toMap(Account::getUuid, a -> a));

        return cases.stream().map(c -> {
            LegalCaseDto dto = mapper.toDto(c);

            Account clientAcc = accounts.get(c.getClientUuid());
            if (clientAcc != null) {
                dto.setClientProfilePictureUrl(convertGcsUrl(clientAcc.getProfilePictureUrl()));
            }

            Account lawyerAcc = accounts.get(c.getLawyerUuid());
            if (lawyerAcc != null) {
                dto.setLawyerProfilePictureUrl(convertGcsUrl(lawyerAcc.getProfilePictureUrl()));
                dto.setLawyerName(extractFullName(lawyerAcc));
            }

            return dto;
        }).collect(Collectors.toList());
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

    @Override
    public LegalCaseDto updateCaseForClient(UUID caseUuid, LegalCaseDto dto, UUID clientUuid) {
        LegalCase existing = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!existing.getClientUuid().equals(clientUuid)) {
            throw new SecurityException("You can only update your own cases");
        }

        // client can only update limited fields
        if (dto.getListing() != null) existing.setListing(dto.getListing());
        if (dto.getCourtDate() != null) existing.setCourtDate(dto.getCourtDate());
        if (dto.getFollowUp() != null) existing.setFollowUp(dto.getFollowUp());
        if (dto.getAvailableTrustFunds() != null) existing.setAvailableTrustFunds(dto.getAvailableTrustFunds());

        if (dto.getCaseTypeId() != null) {
            CaseType caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid caseTypeId"));
            existing.setCaseType(caseType);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        legalCaseRepository.save(existing);

        return mapper.toDto(existing);
    }

    private static String convertGcsUrl(String fileUrl) {
        if (fileUrl == null) return null;

        if (fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }

    private String extractFullName(Account account) {
        if (account == null || account.getPersonalDetails() == null) {
            return "";
        }

        JsonNode pd = account.getPersonalDetails();
        String first = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
        String last  = pd.hasNonNull("lastName")  ? pd.get("lastName").asText()  : "";

        return (first + " " + last).trim();
    }

    @Override
    public List<LegalCaseDto> getCasesByCasePriority(UUID lawyerUuid, int priority) {
        return legalCaseRepository.findAllByLawyerUuidAndCasePriority(lawyerUuid, priority)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegalCaseDto> getCasesByCaseFinalStatus(UUID lawyerUuid, int finalStatus) {
        return legalCaseRepository.findAllByLawyerUuidAndCaseFinalStatus(lawyerUuid, finalStatus)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
