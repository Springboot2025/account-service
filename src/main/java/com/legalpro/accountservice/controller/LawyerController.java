package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientFullResponseDto;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.dto.admin.*;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyer")
@PreAuthorize("hasRole('Lawyer')")
public class LawyerController {

    private final AccountService accountService;
    private final LegalCaseRepository legalCaseRepository;
    private final SuperAdminService superAdminService;

    public LawyerController(AccountService accountService, LegalCaseRepository legalCaseRepository,
                            SuperAdminService superAdminService) {
        this.accountService = accountService;
        this.legalCaseRepository = legalCaseRepository;
        this.superAdminService = superAdminService;
    }

    @GetMapping("/hello")
    public String helloLawyer() {
        return "Hello Lawyer!";
    }

    // --- Update Lawyer Profile ---
    @PutMapping("/{uuid}")
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

    @GetMapping("/companies/{companyUuid}/members")
    public ResponseEntity<ApiResponse<List<LawyerDto>>> getCompanyMembers(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID loggedLawyerUuid = userDetails.getUuid();

        // (Optional) check: only allow access if logged lawyer belongs to this company
        // You can enable this after adding RBAC validation logic later

        List<LawyerDto> members = accountService.getCompanyMembers(companyUuid);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Company members fetched successfully", members)
        );
    }

    @PreAuthorize("hasAnyRole('Lawyer','Admin')")
    @GetMapping("/clients/{clientUuid}")
    public ResponseEntity<ApiResponse<ClientFullResponseDto>> getClientFullDetails(
            @PathVariable UUID clientUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        /*boolean hasCase = legalCaseRepository.existsByClientUuidAndLawyerUuid(clientUuid, lawyerUuid);

        if (!hasCase) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(
                            HttpStatus.FORBIDDEN.value(),
                            "You are not assigned to this client. Access denied."
                    ));
        }*/

        ClientFullResponseDto dto = accountService.getClientFullDetails(clientUuid, lawyerUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Client fetched successfully", dto)
        );
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<InvitationListResponse>> getInvitations(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Invitations fetched successfully",
                        accountService.getInvitations(status, page, size, userDetails)
                )
        );
    }

    @GetMapping("/invitations/summary")
    public ResponseEntity<ApiResponse<InvitationSummaryDto>> getInvitationSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Invitation summary fetched successfully",
                        accountService.getInvitationSummary(userDetails)
                )
        );
    }

    @DeleteMapping("/invitations/{inviteUuid}")
    public ResponseEntity<ApiResponse<String>> cancelInvitation(
            @PathVariable UUID inviteUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        accountService.cancelInvitation(inviteUuid, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Invitation cancelled successfully",
                        null
                )
        );
    }

    @GetMapping("/firm/summary")
    public ResponseEntity<ApiResponse<FirmDashboardSummaryDto>> getFirmSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Firm summary fetched successfully",
                        accountService.getFirmSummary(userDetails)
                )
        );
    }

    @GetMapping("/firmusers")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> getFirmUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Users fetched successfully",
                        accountService.getFormUsers(search, status, location, sort, page, size, userDetails)
                )
        );
    }

    @GetMapping("/firmcases")
    public ResponseEntity<ApiResponse<AdminCaseListResponse>> getFirmCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Firm cases fetched successfully",
                        accountService.getFirmCases(search, status, type, sort, page, size, userDetails)
                )
        );
    }

    @GetMapping("/firmcases/summary")
    public ResponseEntity<ApiResponse<FirmCasesSummaryDto>> getFirmCasesSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Firm case summary fetched successfully",
                        accountService.getFirmCasesSummary(userDetails)
                )
        );
    }
}
