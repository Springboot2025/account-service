package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CaseTypeDto;
import java.util.List;

public interface CaseTypeService {
    CaseTypeDto createCaseType(CaseTypeDto dto);
    List<CaseTypeDto> getAllCaseTypes();
    CaseTypeDto getCaseTypeById(Long id);
}
