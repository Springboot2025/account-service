package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.ContactSummaryDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.Message;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.MessageRepository;
import com.legalpro.accountservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final AccountRepository accountRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final MessageRepository messageRepository;

    @Override
    public Page<ContactSummaryDto> getContactsForLawyer(UUID lawyerUuid, String search, String filter, Pageable pageable) {
        log.info("🎯 Fetching contacts for lawyer={}, filter={}, search={}", lawyerUuid, filter, search);

        // 1️⃣ Get all cases for this lawyer
        List<LegalCase> cases = legalCaseRepository.findAllByLawyerUuid(lawyerUuid);
        if (cases.isEmpty()) return Page.empty(pageable);

        // 2️⃣ Fetch all client accounts involved in these cases
        Set<UUID> clientUuids = cases.stream()
                .map(LegalCase::getClientUuid)
                .collect(Collectors.toSet());

        Map<UUID, Account> clients = clientUuids.stream()
                .map(uuid -> accountRepository.findByUuid(uuid).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Account::getUuid, acc -> acc));

        // 3️⃣ Build ContactSummaryDto list
        List<ContactSummaryDto> summaries = cases.stream()
                .map(legalCase -> {
                    Account client = clients.get(legalCase.getClientUuid());
                    if (client == null) return null;

                    // Last message between lawyer and client
                    Optional<Message> lastMessage = messageRepository
                            .findTopBySenderUuidAndReceiverUuidOrReceiverUuidAndSenderUuidOrderByCreatedAtDesc(
                                    lawyerUuid, client.getUuid(), lawyerUuid, client.getUuid()
                            );

                    Instant lastContact = lastMessage.map(Message::getCreatedAt).orElse(null);

                    String contactName = extractFullName(client.getPersonalDetails());
                    String contactInfo = extractContactInfo(client.getContactInformation());

                    return ContactSummaryDto.builder()
                            .clientUuid(client.getUuid())
                            .contactName(contactName)
                            .caseNumber(legalCase.getCaseNumber())
                            .caseStatus(legalCase.getStatus() != null ? legalCase.getStatus().getName() : "Unknown")
                            .contactInfo(contactInfo)
                            .lastContactDate(lastContact)
                            .reminder(legalCase.getFollowUp())
                            .build();
                })
                .filter(Objects::nonNull)
                .filter(dto -> {
                    // Apply text search on name, case number, or status
                    if (search != null && !search.isBlank()) {
                        String lower = search.toLowerCase();
                        return (dto.getContactName() != null && dto.getContactName().toLowerCase().contains(lower)) ||
                                (dto.getCaseNumber() != null && dto.getCaseNumber().toLowerCase().contains(lower)) ||
                                (dto.getCaseStatus() != null && dto.getCaseStatus().toLowerCase().contains(lower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 4️⃣ Apply filter if needed (like "reminders" or "inactive")
        if (filter != null && !filter.isBlank()) {
            summaries = applyFilter(filter, summaries);
        }

        // 5️⃣ Sorting (based on pageable sort)
        summaries = applySorting(summaries, pageable.getSort());

        // 6️⃣ Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());
        List<ContactSummaryDto> pageContent = summaries.subList(Math.min(start, end), end);

        return new PageImpl<>(pageContent, pageable, summaries.size());
    }

    // --- Helpers ---

    private List<ContactSummaryDto> applyFilter(String filter, List<ContactSummaryDto> list) {
        switch (filter.toLowerCase()) {
            case "reminders":
                return list.stream()
                        .filter(dto -> dto.getReminder() != null && !dto.getReminder().isBlank())
                        .collect(Collectors.toList());
            case "inactive":
                return list.stream()
                        .filter(dto -> dto.getLastContactDate() == null)
                        .collect(Collectors.toList());
            default:
                return list;
        }
    }

    private List<ContactSummaryDto> applySorting(List<ContactSummaryDto> list, Sort sort) {
        if (!sort.isSorted()) return list;

        List<ContactSummaryDto> sorted = new ArrayList<>(list);
        sort.forEach(order -> {
            Comparator<ContactSummaryDto> comparator = switch (order.getProperty()) {
                case "contactName" -> Comparator.comparing(ContactSummaryDto::getContactName, Comparator.nullsLast(String::compareToIgnoreCase));
                case "caseNumber" -> Comparator.comparing(ContactSummaryDto::getCaseNumber, Comparator.nullsLast(String::compareToIgnoreCase));
                case "caseStatus" -> Comparator.comparing(ContactSummaryDto::getCaseStatus, Comparator.nullsLast(String::compareToIgnoreCase));
                case "lastContactDate" -> Comparator.comparing(ContactSummaryDto::getLastContactDate, Comparator.nullsLast(Comparator.naturalOrder()));
                default -> null;
            };
            if (comparator != null) {
                if (order.isDescending()) comparator = comparator.reversed();
                sorted.sort(comparator);
            }
        });
        return sorted;
    }

    private String extractFullName(JsonNode personalDetails) {
        if (personalDetails == null) return "Unknown";
        String first = personalDetails.has("firstName") ? personalDetails.get("firstName").asText() : "";
        String last = personalDetails.has("lastName") ? personalDetails.get("lastName").asText() : "";
        return (first + " " + last).trim();
    }

    private String extractContactInfo(JsonNode contactInfo) {
        if (contactInfo == null) return "";
        String phone = contactInfo.has("phone") ? contactInfo.get("phone").asText() : "";
        String email = contactInfo.has("email") ? contactInfo.get("email").asText() : "";
        return String.join(" / ", Arrays.asList(phone, email).stream().filter(s -> !s.isBlank()).toList());
    }
}
