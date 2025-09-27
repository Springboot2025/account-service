package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer")
@PreAuthorize("hasRole('Lawyer')")
public class LawyerController {

    private final AccountService accountService;

    public LawyerController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/hello")
    public String helloLawyer() {
        return "Hello Lawyer!";
    }

    // --- Update Lawyer Profile ---
    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<LawyerDto>> updateLawyer(
            @PathVariable UUID uuid,
            @RequestBody LawyerDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only update your own profile"));
        }

        Account updated = accountService.updateAccount(uuid, dto, userDetails.getUuid());

        LawyerDto responseDto = AccountMapper.toLawyerDto(updated);

        return ResponseEntity.ok(ApiResponse.success(200, "Updated successfully", responseDto));
    }

    // --- Get Lawyer Profile ---
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<LawyerDto>> getLawyer(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only access your own profile"));
        }

        return accountService.findByUuid(uuid)
                .map(account -> {
                    LawyerDto dto = AccountMapper.toLawyerDto(account);

                    return ResponseEntity.ok(ApiResponse.success(200, "Lawyer fetched successfully", dto));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Lawyer not found")));
    }
}
