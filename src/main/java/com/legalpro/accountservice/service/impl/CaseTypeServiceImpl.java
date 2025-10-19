package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CaseTypeDto;
import com.legalpro.accountservice.entity.CaseType;
import com.legalpro.accountservice.repository.CaseTypeRepository;
import com.legalpro.accountservice.service.CaseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CaseTypeServiceImpl implements CaseTypeService {

    private final CaseTypeRepository caseTypeRepository;

    @Override
    public CaseTypeDto createCaseType(CaseTypeDto dto) {
        caseTypeRepository.findByNameIgnoreCase(dto.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Case type with this name already exists");
        });

        CaseType entity = CaseType.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        CaseType saved = caseTypeRepository.save(entity);

        return CaseTypeDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<CaseTypeDto> getAllCaseTypes() {
        return caseTypeRepository.findAll().stream()
                .map(type -> CaseTypeDto.builder()
                        .id(type.getId())
                        .name(type.getName())
                        .description(type.getDescription())
                        .createdAt(type.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public CaseTypeDto getCaseTypeById(Long id) {
        CaseType type = caseTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case type not found"));

        return CaseTypeDto.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .createdAt(type.getCreatedAt())
                .build();
    }
}
