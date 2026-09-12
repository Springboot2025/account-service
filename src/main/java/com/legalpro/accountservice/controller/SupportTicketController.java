package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.SupportTicketDetailDto;
import com.legalpro.accountservice.dto.admin.AdminTicketListResponse;
import com.legalpro.accountservice.entity.TicketCategory;
import com.legalpro.accountservice.entity.TicketPriority;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.SupportTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Lawyer/client-facing support ticket endpoints -- raising a ticket, viewing
 * and replying to your own tickets. Admin-facing management (list all,
 * filter, change status) lives in SuperAdminController instead, since it's
 * gated by a different role and already follows that controller's
 * conventions.
 */
@RestController
@RequestMapping("/api/support/tickets")
@PreAuthorize("hasAnyRole('Lawyer', 'Client')")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketDetailDto>> createTicket(
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        SupportTicketDetailDto ticket = supportTicketService.createTicket(
                userDetails.getUuid(), subject, description, category, priority, files);
        return ResponseEntity.ok(ApiResponse.success(200, "Ticket submitted successfully", ticket));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<AdminTicketListResponse>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(200, "Tickets fetched successfully",
                supportTicketService.getMyTickets(userDetails.getUuid(), page, size)));
    }

    @GetMapping("/{ticketUuid}")
    public ResponseEntity<ApiResponse<?>> getTicketDetail(
            @PathVariable UUID ticketUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(200, "Ticket fetched successfully",
                    supportTicketService.getTicketDetail(ticketUuid, userDetails.getUuid(), false)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        }
    }

    @PostMapping("/{ticketUuid}/replies")
    public ResponseEntity<ApiResponse<?>> replyToTicket(
            @PathVariable UUID ticketUuid,
            @RequestParam String message,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        try {
            SupportTicketDetailDto ticket = supportTicketService.addReply(
                    ticketUuid, userDetails.getUuid(), message, files, false);
            return ResponseEntity.ok(ApiResponse.success(200, "Reply added successfully", ticket));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
        }
    }
}
