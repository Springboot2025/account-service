package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LegalCaseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LegalCaseService {

    LegalCaseDto createCase(LegalCaseDto dto, UUID lawyerUuid);

    LegalCaseDto updateCase(UUID caseUuid, LegalCaseDto dto, UUID lawyerUuid);

    LegalCaseDto getCase(UUID caseUuid, UUID lawyerUuid);

    List<LegalCaseDto> getCasesForLawyer(UUID lawyerUuid);

    void deleteCase(UUID caseUuid, UUID lawyerUuid);

    List<LegalCaseDto> getCasesByStatus(UUID lawyerUuid, String statusName);

    Map<String, Object> getCaseSummary(UUID lawyerUuid);

    List<LegalCaseDto> getCasesByType(UUID lawyerUuid, String typeName);

    LegalCaseDto createCaseForClient(LegalCaseDto dto, UUID clientUuid);
    List<LegalCaseDto> getCasesForClient(UUID clientUuid);
    LegalCaseDto getCaseForClient(UUID caseUuid, UUID clientUuid);
    List<LegalCaseDto> getCasesByStatusForClient(UUID clientUuid, String statusName);
    Map<String, Object> getCaseSummaryForClient(UUID clientUuid);
    List<LegalCaseDto> getCasesByTypeForClient(UUID clientUuid, String typeName);
}
