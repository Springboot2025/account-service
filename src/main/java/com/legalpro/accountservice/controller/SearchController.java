package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.dto.LawyerSearchGroupedResponse;
import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.service.LawyerSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final LawyerSearchService lawyerSearchService;

    /**
     * Public API to search for lawyers by filters such as name, location, email, or phone.
     * Does not require authentication or any roles.
     */
    @PostMapping("/lawyers")
    public ResponseEntity<ApiResponse<Page<LawyerDto>>> searchLawyers(
            @RequestBody LawyerSearchRequestDto request
    ) {

        Page<LawyerDto> result = lawyerSearchService.searchLawyers(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Lawyers fetched successfully",
                        result
                )
        );
    }

}
