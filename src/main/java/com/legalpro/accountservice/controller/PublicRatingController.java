package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerRatingDto;
import com.legalpro.accountservice.service.LawyerRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class PublicRatingController {

    private final LawyerRatingService lawyerRatingService;

    // --- Get all ratings for a specific lawyer ---
    @GetMapping("/lawyer/{lawyerUuid}")
    public ResponseEntity<ApiResponse<List<LawyerRatingDto>>> getRatingsForLawyer(
            @PathVariable UUID lawyerUuid
    ) {
        List<LawyerRatingDto> ratings = lawyerRatingService.getRatingsByLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Lawyer ratings fetched successfully", ratings));
    }

    // --- Get average rating for a specific lawyer ---
    @GetMapping("/lawyer/{lawyerUuid}/average")
    public ResponseEntity<ApiResponse<BigDecimal>> getAverageRatingForLawyer(
            @PathVariable UUID lawyerUuid
    ) {
        BigDecimal avg = lawyerRatingService.getAverageRatingForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Average rating fetched successfully", avg));
    }
}
