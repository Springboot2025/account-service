package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.entity.CaseEvent;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.CaseEventMapper;
import com.legalpro.accountservice.repository.CaseEventRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.ActivityLogService;
import com.legalpro.accountservice.service.CaseEventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaseEventServiceImpl implements CaseEventService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseEventRepository caseEventRepository;
    private final CaseEventMapper caseEventMapper;
    private final ActivityLogService activityLogService;

    public CaseEventServiceImpl(LegalCaseRepository legalCaseRepository,
                                CaseEventRepository caseEventRepository,
                                CaseEventMapper caseEventMapper,
                                ActivityLogService activityLogService) {
        this.legalCaseRepository = legalCaseRepository;
        this.caseEventRepository = caseEventRepository;
        this.caseEventMapper = caseEventMapper;
        this.activityLogService = activityLogService;
    }

    @Override
    public CaseEventDto createCaseEvent(UUID caseUuid, UUID lawyerUuid, String lawyerName, CaseEventDto dto) {

        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new AccessDeniedException("You can only manage events for your own cases");
        }

        CaseEvent entity = caseEventMapper.toEntity(dto);
        entity.setCaseUuid(caseUuid);
        entity.setUserName(lawyerName);

        CaseEvent saved = caseEventRepository.save(entity);

        activityLogService.logActivity(
                "CASE_EVENT_ADDED",
                "New case event added",
                lawyerUuid,                        // actorUuid
                lawyerUuid,                        // lawyerUuid
                legalCase.getClientUuid(),         // clientUuid
                caseUuid,                          // caseUuid
                saved.getUuid(),                   // referenceUuid
                Map.of(                             // metadata
                        "eventType", saved.getEventType(),
                        "title", saved.getTitle(),
                        "status", saved.getStatus(),
                        "eventDate", saved.getEventDate()
                )
        );


        return caseEventMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseEventDto> getCaseEvents(UUID caseUuid, UUID lawyerUuid) {

        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new AccessDeniedException("You can only view events for cases you own");
        }

        return caseEventRepository
                .findAllByCaseUuidAndDeletedAtIsNullOrderByEventDateDesc(caseUuid)
                .stream()
                .map(caseEventMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseEventDto> getClientCaseEvents(UUID caseUuid, UUID clientUuid) {

        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (!legalCase.getClientUuid().equals(clientUuid)) {
            throw new AccessDeniedException("You can only view events for cases you own");
        }

        return caseEventRepository
                .findAllByCaseUuidAndDeletedAtIsNullOrderByEventDateDesc(caseUuid)
                .stream()
                .map(caseEventMapper::toDto)
                .toList();
    }

    @Override
    public List<CaseEventDto> getUpcomingEvents(UUID lawyerUuid, Integer limit) {

        Pageable pageable = (limit != null && limit > 0)
                ? PageRequest.of(0, limit)
                : Pageable.unpaged();

        List<CaseEvent> events = caseEventRepository.findUpcomingEvents(
                lawyerUuid,
                LocalDate.now(),
                pageable
        );

        return events.stream()
                .map(caseEventMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CaseEventDto updateCaseEvent(UUID eventUuid, UUID lawyerUuid, CaseEventDto dto) {

        CaseEvent event = caseEventRepository.findByUuidAndDeletedAtIsNull(eventUuid)
                .orElseThrow(() -> new RuntimeException("Case event not found"));

        event.setEventDate(dto.getDate());
        event.setEventType(dto.getType());
        event.setTitle(dto.getTitle());
        event.setDetails(dto.getDetails());
        event.setStatus(dto.getStatus());
        event.setRelatedDate(dto.getRelatedDate());

        CaseEvent saved = caseEventRepository.save(event);
        return caseEventMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCaseEvent(UUID eventUuid, UUID lawyerUuid) {

        CaseEvent event = caseEventRepository.findByUuidAndDeletedAtIsNull(eventUuid)
                .orElseThrow(() -> new RuntimeException("Case event not found"));

        caseEventRepository.softDeleteByUuid(eventUuid);
    }

    @Override
    public List<CaseEventDto> getClientUpcomingEvents(UUID clientUuid, Integer limit) {

        int finalLimit = (limit == null || limit <= 0) ? 5 : limit;

        Pageable pageable = PageRequest.of(0, finalLimit);

        List<CaseEvent> events =
                caseEventRepository.findUpcomingEventsForClient(clientUuid, pageable);

        return events.stream()
                .map(caseEventMapper::toDto)
                .collect(Collectors.toList());
    }
}
