package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LetterOfAdviceTemplateDto;
import com.legalpro.accountservice.dto.LetterOfAdviceTemplateRequest;

import java.util.List;
import java.util.UUID;

public interface LetterOfAdviceTemplateService {

    List<LetterOfAdviceTemplateDto> getTemplatesForLawyer(UUID lawyerUuid);

    LetterOfAdviceTemplateDto getTemplate(UUID lawyerUuid, UUID templateUuid);

    LetterOfAdviceTemplateDto createTemplate(UUID lawyerUuid, LetterOfAdviceTemplateRequest request);

    LetterOfAdviceTemplateDto updateTemplate(UUID lawyerUuid, UUID templateUuid, LetterOfAdviceTemplateRequest request);

    void deleteTemplate(UUID lawyerUuid, UUID templateUuid);
}
