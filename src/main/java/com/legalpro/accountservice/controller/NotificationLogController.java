package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.NotificationLogDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Client','Lawyer','Admin')") // ✅ Applies to all endpoints in this controller
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    /**
     * Get all notifications sent to a user (most recent first)
     */
    @GetMapping("/{userUuid}")
    public ResponseEntity<ApiResponse<List<NotificationLogDto>>> getLogsForUser(
            @PathVariable UUID userUuid,
            @AuthenticationPrincipal Object userDetails // optional
    ) {
        List<NotificationLogDto> logs = notificationLogService.getLogsByUser(userUuid);

        if (logs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No notification logs found for this user"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Notification logs fetched successfully",
                logs
        ));
    }

    /**
     * Mark a single notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationLogService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Notification marked as read",
                "OK"
        ));
    }

    /**
     * Mark all notifications for a user as read
     */
    @PutMapping("/{userUuid}/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(@PathVariable UUID userUuid) {
        notificationLogService.markAllAsRead(userUuid);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "All notifications marked as read",
                "OK"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOne(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationLogService.deleteOne(id, userDetails.getUuid());

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Notification deleted successfully",
                "OK"
        ));
    }

    @DeleteMapping("/user/{userUuid}")
    public ResponseEntity<ApiResponse<String>> deleteAll(
            @PathVariable UUID userUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!userUuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(
                            HttpStatus.FORBIDDEN.value(),
                            "You can only delete your own notifications"
                    ));
        }

        notificationLogService.deleteAll(userUuid);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "All notifications deleted successfully",
                "OK"
        ));
    }

}
