package com.legalpro.accountservice.service;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    /**
     * Send a push notification to a single FCM device token.
     */
    String sendNotification(UUID userUuid, String token, String title, String body);

    /**
     * Send a push notification with optional custom data payload.
     */
    String sendNotificationWithData(UUID userUuid, String token, String title, String body, Map<String, String> data);
}
