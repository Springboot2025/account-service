package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.MessageDto;
import com.legalpro.accountservice.dto.MessageRequestDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.MessageService;
import com.legalpro.accountservice.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Client','Lawyer','Admin')")
public class MessageController {

    private final MessageService messageService;

    // === Send message ===
    @PostMapping
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @RequestBody MessageRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MessageDto message = messageService.sendMessage(
                userDetails.getUuid(),
                request.getReceiverUuid(),
                request.getContent()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    // === Get conversation ===
    @GetMapping("/conversation/{otherUuid}")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getConversation(
            @PathVariable UUID otherUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MessageDto> conversation = messageService.getConversation(userDetails.getUuid(), otherUuid);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    // === Unread count ===
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = messageService.countUnread(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // === Mark as read ===
    @PutMapping("/mark-read/{otherUuid}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID otherUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        messageService.markAsRead(userDetails.getUuid(), otherUuid);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
