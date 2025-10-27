package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerRatingDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LawyerRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client/ratings")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientRatingController {

    private final LawyerRatingService lawyerRatingService;

    // --- Create or update a rating ---
    @PostMapping
    public ResponseEntity<ApiResponse<LawyerRatingDto>> createOrUpdateRating(
            @RequestBody LawyerRatingDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();

        // Ownership check
        if (dto.getClientUuid() != null && !dto.getClientUuid().equals(clientUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(),
                            "You can only submit ratings as yourself"));
        }

        LawyerRatingDto saved = lawyerRatingService.createOrUpdateRating(dto, clientUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(),
                        "Rating submitted successfully", saved));
    }

    // --- Get all ratings by this client ---
    @GetMapping("/{clientUuid}")
    public ResponseEntity<ApiResponse<List<LawyerRatingDto>>> getRatingsByClient(
            @PathVariable UUID clientUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!clientUuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(),
                            "You can only view your own ratings"));
        }

        List<LawyerRatingDto> ratings = lawyerRatingService.getRatingsByClient(clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Ratings fetched successfully", ratings));
    }

    // --- Delete a rating (soft delete) ---
    @DeleteMapping("/{ratingUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable UUID ratingUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        lawyerRatingService.deleteRating(ratingUuid, clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Rating deleted successfully", null));
    }
}
