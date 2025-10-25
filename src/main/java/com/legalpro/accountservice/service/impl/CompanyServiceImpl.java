package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CompanyDto;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.mapper.CompanyMapper;
import com.legalpro.accountservice.repository.CompanyRepository;
import com.legalpro.accountservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper mapper;

    @Override
    public CompanyDto createCompany(CompanyDto dto, UUID lawyerUuid) {
        Company company = Company.builder()
                .uuid(UUID.randomUUID())
                .name(dto.getName())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        companyRepository.save(company);
        log.info("Company created by lawyer [{}]: {}", lawyerUuid, company.getName());
        return mapper.toDto(company);
    }

    @Override
    public CompanyDto updateCompany(UUID companyUuid, CompanyDto dto, UUID lawyerUuid) {
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getDescription() != null) company.setDescription(dto.getDescription());
        company.setUpdatedAt(LocalDateTime.now());

        companyRepository.save(company);
        log.info("Company [{}] updated by lawyer [{}]", companyUuid, lawyerUuid);
        return mapper.toDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompany(UUID companyUuid) {
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        return mapper.toDto(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCompany(UUID companyUuid, UUID lawyerUuid) {
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setRemovedAt(LocalDateTime.now());
        companyRepository.save(company);
        log.info("Company [{}] deleted (soft) by lawyer [{}]", companyUuid, lawyerUuid);
    }
}
