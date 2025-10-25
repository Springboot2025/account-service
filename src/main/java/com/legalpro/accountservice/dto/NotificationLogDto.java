package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationLogDto {
    private Long id;
    private UUID userUuid;
    private String deviceId;
    private String fcmToken;
    private String messageId;
    private String title;
    private String body;
    private String payload;
    private String status;
    private String errorMessage;
    private ZonedDateTime sentAt;
}
