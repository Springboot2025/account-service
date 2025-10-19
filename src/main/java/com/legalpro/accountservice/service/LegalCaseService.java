package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LegalCaseDto;

import java.util.List;
import java.util.UUID;

public interface LegalCaseService {

    LegalCaseDto createCase(LegalCaseDto dto, UUID lawyerUuid);

    LegalCaseDto updateCase(UUID caseUuid, LegalCaseDto dto, UUID lawyerUuid);

    LegalCaseDto getCase(UUID caseUuid, UUID lawyerUuid);

    List<LegalCaseDto> getCasesForLawyer(UUID lawyerUuid);

    void deleteCase(UUID caseUuid, UUID lawyerUuid);
}
