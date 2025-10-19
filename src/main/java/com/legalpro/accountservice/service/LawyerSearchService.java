package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import org.springframework.data.domain.Page;

public interface LawyerSearchService {
    Page<LawyerDto> searchLawyers(LawyerSearchRequestDto request);
}
