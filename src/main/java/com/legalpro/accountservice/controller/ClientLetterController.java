package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientLetterDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ClientLetterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/letters")
@PreAuthorize("hasRole('Client')")
public class ClientLetterController {

    private final ClientLetterService clientLetterService;

    public ClientLetterController(ClientLetterService clientLetterService) {
        this.clientLetterService = clientLetterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientLetterDto>>> getLetters(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        List<ClientLetterDto> letters =
                clientLetterService.getClientLetters(userDetails.getUuid());

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Letters fetched successfully",
                        letters
                )
        );
    }
}
