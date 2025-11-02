package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.SubscriberDto;
import com.legalpro.accountservice.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;

    // --- Public: Add new subscriber (Website Form) ---
    @PostMapping("/subscribers")
    public ResponseEntity<ApiResponse<SubscriberDto>> addSubscriber(@RequestBody SubscriberDto dto) {
        log.info("📩 Received new subscription: {}", dto.getEmail());
        SubscriberDto saved = subscriberService.addSubscriber(dto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Subscriber added successfully", saved));
    }

    // --- Admin: Get all subscribers ---
    @GetMapping("/admin/subscribers")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<List<SubscriberDto>>> getAllSubscribers() {
        List<SubscriberDto> subscribers = subscriberService.getAllSubscribers();
        return ResponseEntity.ok(ApiResponse.success(200, "Subscribers fetched successfully", subscribers));
    }

    // --- Admin: Deactivate (Unsubscribe) ---
    @DeleteMapping("/admin/subscribers/{uuid}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Void>> deactivateSubscriber(@PathVariable UUID uuid) {
        subscriberService.deactivateSubscriber(uuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Subscriber deactivated successfully", null));
    }

    // --- Admin: Send Notification to Subscribers ---
    // --- Admin: Send Notification to Subscribers ---
    @PostMapping("/admin/subscribers/notify")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @RequestBody NotificationRequest request
    ) {
        subscriberService.sendNotificationToSubscribers(
                request.subscriberUuids(),
                request.subject(),
                request.messageBody()
        );
        return ResponseEntity.ok(ApiResponse.success(200, "Notifications sent successfully", null));
    }


    // ✅ Simple DTO for notification requests
    public record NotificationRequest(
            List<UUID> subscriberUuids,
            String subject,
            String messageBody
    ) {}
}
