package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CaseEventDto;

import java.util.List;
import java.util.UUID;

public interface CaseEventService {

    CaseEventDto createCaseEvent(UUID caseUuid, UUID lawyerUuid, String lawyerName, CaseEventDto dto);

    List<CaseEventDto> getCaseEvents(UUID caseUuid, UUID lawyerUuid);
    List<CaseEventDto> getClientCaseEvents(UUID caseUuid, UUID clientUuid);
}
