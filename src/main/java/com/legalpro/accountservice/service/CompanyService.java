package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CompanyDto;

import java.util.List;
import java.util.UUID;

public interface CompanyService {

    CompanyDto createCompany(CompanyDto dto, UUID lawyerUuid);

    CompanyDto updateCompany(UUID companyUuid, CompanyDto dto, UUID lawyerUuid);

    CompanyDto getCompany(UUID companyUuid);

    List<CompanyDto> getAllCompanies();

    void deleteCompany(UUID companyUuid, UUID lawyerUuid);
}
