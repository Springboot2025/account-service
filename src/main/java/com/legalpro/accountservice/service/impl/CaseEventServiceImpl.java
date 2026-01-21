package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CaseEventDto;
import com.legalpro.accountservice.entity.CaseEvent;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.mapper.CaseEventMapper;
import com.legalpro.accountservice.repository.CaseEventRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.service.CaseEventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CaseEventServiceImpl implements CaseEventService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseEventRepository caseEventRepository;
    private final CaseEventMapper caseEventMapper;

    public CaseEventServiceImpl(LegalCaseRepository legalCaseRepository,
                                CaseEventRepository caseEventRepository,
                                CaseEventMapper caseEventMapper) {
        this.legalCaseRepository = legalCaseRepository;
        this.caseEventRepository = caseEventRepository;
        this.caseEventMapper = caseEventMapper;
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
                LocalDateTime.now(),
                pageable
        );

        return events.stream()
                .map(caseEventMapper::toDto)
                .toList();
    }

}
