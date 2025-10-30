package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.NotificationLogDto;
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
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    /**
     * Get all notifications sent to a user (most recent first)
     */
    @GetMapping("/{userUuid}")
    @PreAuthorize("hasAnyRole('Client','Lawyer','Admin')")
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
}