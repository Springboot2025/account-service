package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.UserFeedbackDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.UserFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class UserFeedbackController {

    private final UserFeedbackService userFeedbackService;

    // === Submit Feedback (Client or Lawyer) ===
    @PostMapping
    @PreAuthorize("hasAnyRole('Client','Lawyer')")
    public ResponseEntity<ApiResponse<UserFeedbackDto>> submitFeedback(
            @RequestBody UserFeedbackDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userUuid = userDetails.getUuid();
        UserFeedbackDto saved = userFeedbackService.submitFeedback(userUuid, dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Feedback submitted successfully", saved));
    }

    // === Get Feedback (Public Testimonials) ===
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserFeedbackDto>>> getFeedback() {
        List<UserFeedbackDto> feedbacks = userFeedbackService.getPublicFeedback();
        return ResponseEntity.ok(ApiResponse.success(200, "Feedback fetched successfully", feedbacks));
    }

    // === Get My Feedback (Authenticated User) ===
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('Client','Lawyer')")
    public ResponseEntity<ApiResponse<List<UserFeedbackDto>>> getMyFeedback(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userUuid = userDetails.getUuid();
        List<UserFeedbackDto> feedbacks = userFeedbackService.getMyFeedback(userUuid);

        return ResponseEntity.ok(ApiResponse.success(200, "My feedback fetched successfully", feedbacks));
    }
}
