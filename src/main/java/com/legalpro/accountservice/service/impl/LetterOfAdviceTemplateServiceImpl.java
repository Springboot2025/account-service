package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LetterOfAdviceTemplateDto;
import com.legalpro.accountservice.dto.LetterOfAdviceTemplateRequest;
import com.legalpro.accountservice.entity.LetterOfAdviceTemplate;
import com.legalpro.accountservice.mapper.LetterOfAdviceTemplateMapper;
import com.legalpro.accountservice.repository.LetterOfAdviceTemplateRepository;
import com.legalpro.accountservice.service.LetterOfAdviceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LetterOfAdviceTemplateServiceImpl implements LetterOfAdviceTemplateService {

    private final LetterOfAdviceTemplateRepository templateRepository;
    private final LetterOfAdviceTemplateMapper templateMapper;

    @Override
    public List<LetterOfAdviceTemplateDto> getTemplatesForLawyer(UUID lawyerUuid) {
        return templateRepository.findAllByLawyerUuidAndDeletedAtIsNullOrderByUpdatedAtDesc(lawyerUuid)
                .stream()
                .map(templateMapper::toDto)
                .toList();
    }

    @Override
    public LetterOfAdviceTemplateDto getTemplate(UUID lawyerUuid, UUID templateUuid) {
        LetterOfAdviceTemplate entity = findOwnedTemplate(lawyerUuid, templateUuid);
        return templateMapper.toDto(entity);
    }

    @Override
    @Transactional
    public LetterOfAdviceTemplateDto createTemplate(UUID lawyerUuid, LetterOfAdviceTemplateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Template name is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Template content is required");
        }

        LetterOfAdviceTemplate entity = LetterOfAdviceTemplate.builder()
                .lawyerUuid(lawyerUuid)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .content(request.getContent())
                .build();

        return templateMapper.toDto(templateRepository.save(entity));
    }

    @Override
    @Transactional
    public LetterOfAdviceTemplateDto updateTemplate(UUID lawyerUuid, UUID templateUuid, LetterOfAdviceTemplateRequest request) {
        LetterOfAdviceTemplate entity = findOwnedTemplate(lawyerUuid, templateUuid);

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        if (request.getContent() != null) entity.setContent(request.getContent());

        return templateMapper.toDto(templateRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID lawyerUuid, UUID templateUuid) {
        LetterOfAdviceTemplate entity = findOwnedTemplate(lawyerUuid, templateUuid);
        entity.setDeletedAt(LocalDateTime.now());
        templateRepository.save(entity);
    }

    private LetterOfAdviceTemplate findOwnedTemplate(UUID lawyerUuid, UUID templateUuid) {
        return templateRepository.findByUuidAndLawyerUuidAndDeletedAtIsNull(templateUuid, lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }
}
