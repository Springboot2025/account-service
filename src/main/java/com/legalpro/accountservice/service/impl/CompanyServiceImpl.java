package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CompanyDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.mapper.CompanyMapper;
import com.legalpro.accountservice.repository.AccountRepository;
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
    private final AccountRepository accountRepository;
    private final CompanyMapper mapper;

    @Override
    public CompanyDto createCompany(CompanyDto dto, UUID lawyerUuid) {
        Company company = Company.builder()
                .uuid(UUID.randomUUID())
                .name(dto.getName())
                .description(dto.getDescription())
                .abn(dto.getAbn())
                .businessPhone(dto.getBusinessPhone())
                .businessAddress(dto.getBusinessAddress())
                .primaryContactName(dto.getPrimaryContactName())
                .primaryContactEmail(dto.getPrimaryContactEmail())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        companyRepository.save(company);

        // The lawyer creating the company becomes its firm admin
        Account lawyer = accountRepository.findByUuid(lawyerUuid).orElse(null);
        if (lawyer != null) {
            lawyer.setCompany(true);
            lawyer.setCompanyUuid(company.getUuid());
            accountRepository.save(lawyer);
        }

        log.info("Company created by lawyer [{}]: {}", lawyerUuid, company.getName());
        return mapper.toDto(company);
    }

    @Override
    public CompanyDto updateCompany(UUID companyUuid, CompanyDto dto, UUID lawyerUuid) {
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getDescription() != null) company.setDescription(dto.getDescription());
        if (dto.getAbn() != null) company.setAbn(dto.getAbn());
        if (dto.getBusinessPhone() != null) company.setBusinessPhone(dto.getBusinessPhone());
        if (dto.getBusinessAddress() != null) company.setBusinessAddress(dto.getBusinessAddress());
        if (dto.getPrimaryContactName() != null) company.setPrimaryContactName(dto.getPrimaryContactName());
        if (dto.getPrimaryContactEmail() != null) company.setPrimaryContactEmail(dto.getPrimaryContactEmail());
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
