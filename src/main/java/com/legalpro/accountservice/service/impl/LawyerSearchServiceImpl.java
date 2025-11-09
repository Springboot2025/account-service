package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LawyerRatingRepository;
import com.legalpro.accountservice.service.LawyerSearchService;
import com.legalpro.accountservice.specification.LawyerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LawyerSearchServiceImpl implements LawyerSearchService {

    private final AccountRepository accountRepository;
    private final LawyerRatingRepository lawyerRatingRepository; // ✅ ADD THIS
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

        // ✅ Convert Accounts → LawyerDto
        List<LawyerDto> lawyerDtos = lawyerPage.getContent().stream()
                .map(AccountMapper::toLawyerDto)
                .collect(Collectors.toList());

        // ✅ Extract UUIDs for bulk lookup
        List<UUID> uuids = lawyerDtos.stream()
                .map(LawyerDto::getUuid)
                .toList();

        if (!uuids.isEmpty()) {
            // ✅ Bulk load average ratings
            List<Object[]> avgList = lawyerRatingRepository.getAverageRatingsForLawyers(uuids);

            Map<UUID, BigDecimal> avgMap = avgList.stream()
                    .collect(Collectors.toMap(
                            row -> (UUID) row[0],
                            row -> {
                                Object val = row[1];
                                if (val instanceof BigDecimal bd) return bd;
                                if (val instanceof Double d) return BigDecimal.valueOf(d);
                                if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
                                return BigDecimal.ZERO;
                            }
                    ));

            // ✅ Attach to DTO
            lawyerDtos.forEach(dto ->
                    dto.setAverageRating(avgMap.getOrDefault(dto.getUuid(), BigDecimal.ZERO))
            );
        }

        return new PageImpl<>(lawyerDtos, pageable, lawyerPage.getTotalElements());
    }
}
