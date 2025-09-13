package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.entity.Account;
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

    // Add Lawyer-specific APIs here
    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountDto>> updateLawyer(
            @PathVariable UUID uuid,
            @RequestBody AccountDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Account updated = accountService.updateAccount(uuid, dto, userDetails.getUuid());

        AccountDto responseDto = AccountDto.builder()
                .id(updated.getId())
                .uuid(updated.getUuid())
                .firstName(updated.getFirstName())
                .lastName(updated.getLastName())
                .gender(updated.getGender())
                .email(updated.getEmail())
                .mobile(updated.getMobile())
                .address(updated.getAddress())
                .build();

        return ResponseEntity.ok(ApiResponse.success(200, "Updated successfully", responseDto));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountDto>> getClient(
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
                    AccountDto dto = AccountDto.builder()
                            .id(account.getId())
                            .uuid(account.getUuid())
                            .firstName(account.getFirstName())
                            .lastName(account.getLastName())
                            .gender(account.getGender())
                            .email(account.getEmail())
                            .mobile(account.getMobile())
                            .address(account.getAddress())
                            .build();

                    return ResponseEntity.ok(ApiResponse.success(200, "Lawyer fetched successfully", dto));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Lawyer not found")));
    }

}
