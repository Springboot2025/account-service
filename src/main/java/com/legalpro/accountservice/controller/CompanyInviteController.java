package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CompanyInviteDto;
import com.legalpro.accountservice.dto.CompanyInviteRequestDto;
import com.legalpro.accountservice.entity.CompanyInvite;
import com.legalpro.accountservice.mapper.CompanyInviteMapper;
import com.legalpro.accountservice.service.CompanyInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class CompanyInviteController {

    private final CompanyInviteService inviteService;

    // Company sends invite to lawyer
    @PostMapping
    @PreAuthorize("hasRole('Lawyer')") // Only company owners
    public ResponseEntity<ApiResponse<CompanyInviteDto>> createInvite(
            @RequestBody CompanyInviteRequestDto request) {

        CompanyInvite invite = inviteService.createInvite(request);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Invite created", CompanyInviteMapper.toDto(invite))
        );
    }

    // Public validation for the registration screen
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<CompanyInviteDto>> validateInvite(
            @RequestParam String token) {

        CompanyInvite invite = inviteService.validateToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Invite valid", CompanyInviteMapper.toDto(invite))
        );
    }
}
