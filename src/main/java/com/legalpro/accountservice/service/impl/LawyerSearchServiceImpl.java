package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.*;
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
    public LawyerSearchGroupedResponse searchLawyers(LawyerSearchRequestDto request) {

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 10;

        var spec = LawyerSpecification.build(request);

        // STEP 1 — Fetch ALL matching lawyers (no pagination)
        List<Account> lawyers = accountRepository.findAll(spec);

        // STEP 2 — Convert to DTO
        List<LawyerDto> lawyerDtos = lawyers.stream()
                .map(AccountMapper::toLawyerDto)
                .toList();

        // STEP 3 — Group hierarchically
        Map<String, Map<String, Map<String, List<LawyerDto>>>> grouped =
                lawyerDtos.stream()
                        .collect(Collectors.groupingBy(
                                dto -> getState(dto),
                                Collectors.groupingBy(
                                        dto -> getPostcode(dto),
                                        Collectors.groupingBy(
                                                dto -> getSuburb(dto)
                                        )
                                )
                        ));

        // STEP 4 — Convert Map → Nested DTO Structure
        List<StateGroup> stateGroups = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Sort by state name
                .map(stateEntry -> {

                    String stateName = stateEntry.getKey();
                    Map<String, Map<String, List<LawyerDto>>> postcodeMap = stateEntry.getValue();

                    List<PostcodeGroup> postcodeGroups = postcodeMap.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(postcodeEntry -> {

                                String postcode = postcodeEntry.getKey();
                                Map<String, List<LawyerDto>> suburbMap = postcodeEntry.getValue();

                                List<SuburbGroup> suburbGroups = suburbMap.entrySet().stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .map(suburbEntry -> SuburbGroup.builder()
                                                .suburb(suburbEntry.getKey())
                                                .lawyers(suburbEntry.getValue())
                                                .build())
                                        .toList();

                                return PostcodeGroup.builder()
                                        .postcode(postcode)
                                        .suburbs(suburbGroups)
                                        .build();
                            })
                            .toList();

                    long totalLawyersInState = postcodeMap.values().stream()
                            .flatMap(postcode -> postcode.values().stream())
                            .mapToLong(List::size)
                            .sum();

                    return StateGroup.builder()
                            .state(stateName)
                            .totalLawyers(totalLawyersInState)
                            .postcodes(postcodeGroups)
                            .build();
                })
                .toList();

        // STEP 5 — Paginate at STATE level
        int totalStates = stateGroups.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalStates);

        List<StateGroup> pagedStates = fromIndex >= totalStates
                ? Collections.emptyList()
                : stateGroups.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) totalStates / size);

        return LawyerSearchGroupedResponse.builder()
                .states(pagedStates)
                .totalElements(totalStates)
                .totalPages(totalPages)
                .build();
    }

    private String getState(LawyerDto dto) {
        Object state = dto.getAddressDetails().get("state_province");
        return state != null ? state.toString() : "Unknown";
    }

    private String getPostcode(LawyerDto dto) {
        Object postcode = dto.getAddressDetails().get("postcode");
        return postcode != null ? postcode.toString() : "Unknown";
    }

    private String getSuburb(LawyerDto dto) {
        Object suburb = dto.getAddressDetails().get("city_suburb");
        return suburb != null ? suburb.toString() : "Unknown";
    }
}
