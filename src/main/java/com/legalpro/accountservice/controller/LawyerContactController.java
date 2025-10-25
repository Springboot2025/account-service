package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ContactSummaryDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/contacts")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerContactController {

    private final ContactService contactService;

    /**
     * Unified contact API:
     * ✅ Works with and without filters
     * ✅ Supports pagination, sorting, and search
     *
     * Examples:
     * - GET /api/lawyer/contacts
     * - GET /api/lawyer/contacts?filter=reminders&page=0&size=10
     * - GET /api/lawyer/contacts?search=john
     * - GET /api/lawyer/contacts?sort=lastContact,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContactSummaryDto>>> getContactsForLawyer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "lastContact,desc") String sort
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        log.info("📞 Fetching contacts for lawyer {}, filter={}, search={}, sort={}", lawyerUuid, filter, search, sort);

        // Parse sort param
        Sort sortSpec = parseSort(sort);

        // If no filters or pagination → fetch all
        boolean noFilters = (page == null && size == null &&
                (filter == null || filter.isBlank()) &&
                (search == null || search.isBlank()));

        if (noFilters) {
            var allContacts = contactService
                    .getContactsForLawyer(lawyerUuid, null, null, PageRequest.of(0, Integer.MAX_VALUE, sortSpec))
                    .getContent();

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(),
                            "All contacts fetched successfully",
                            new PageImpl<>(allContacts))
            );
        }

        int safePage = (page != null) ? page : 0;
        int safeSize = (size != null) ? size : 10;

        Page<ContactSummaryDto> contacts = contactService.getContactsForLawyer(
                lawyerUuid, search, filter, PageRequest.of(safePage, safeSize, sortSpec)
        );

        if (contacts.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.NO_CONTENT.value(),
                            "No contacts found", contacts)
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(),
                        "Contacts fetched successfully", contacts)
        );
    }

    private Sort parseSort(String sortParam) {
        try {
            String[] parts = sortParam.split(",");
            String field = parts[0];
            Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return Sort.by(direction, field);
        } catch (Exception e) {
            log.warn("⚠️ Invalid sort param '{}', falling back to default", sortParam);
            return Sort.by(Sort.Direction.DESC, "lastContact");
        }
    }
}
