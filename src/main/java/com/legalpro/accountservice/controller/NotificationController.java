package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.entity.DeviceToken;
import com.legalpro.accountservice.service.DeviceTokenService;
import com.legalpro.accountservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    // --- Send plain notification to one user (all devices) ---
    @PostMapping("/send/{userUuid}")
    public ResponseEntity<ApiResponse<String>> sendNotification(
            @PathVariable UUID userUuid,
            @RequestParam String title,
            @RequestParam String body
    ) {
        var tokens = deviceTokenService.getTokensForUser(userUuid);
        tokens.forEach(token ->
                notificationService.sendNotification(token.getFcmToken(), title, body)
        );
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Notifications sent successfully",
                null
        ));
    }

    // --- Send notification with data payload ---
    @PostMapping("/send-data/{userUuid}")
    public ResponseEntity<ApiResponse<String>> sendNotificationWithData(
            @PathVariable UUID userUuid,
            @RequestParam String title,
            @RequestParam String body,
            @RequestBody Map<String, String> data
    ) {
        var tokens = deviceTokenService.getTokensForUser(userUuid);
        tokens.forEach(token ->
                notificationService.sendNotificationWithData(token.getFcmToken(), title, body, data)
        );
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Notifications with data sent successfully",
                null
        ));
    }
}
