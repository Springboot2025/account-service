package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.PublicLawyerProfileDto;
import com.legalpro.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/lawyers")
@RequiredArgsConstructor
public class PublicLawyerController {

    private final AccountService accountService;

    @GetMapping("/{lawyerUuid}")
    public ResponseEntity<ApiResponse<PublicLawyerProfileDto>> getPublicLawyerProfile(
            @PathVariable UUID lawyerUuid
    ) {

        PublicLawyerProfileDto dto =
                accountService.getPublicLawyerProfile(lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Lawyer public profile fetched successfully",
                        dto
                )
        );
    }
}
