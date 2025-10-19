package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.service.LawyerSearchService;
import com.legalpro.accountservice.specification.LawyerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LawyerSearchServiceImpl implements LawyerSearchService {

    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<LawyerDto> searchLawyers(LawyerSearchRequestDto request) {

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        var spec = LawyerSpecification.build(request);

        Page<Account> lawyerPage = accountRepository.findAll(spec, pageable);

        return new PageImpl<>(
                lawyerPage.stream()
                        .map(AccountMapper::toLawyerDto)
                        .collect(Collectors.toList()),
                pageable,
                lawyerPage.getTotalElements()
        );
    }
}
