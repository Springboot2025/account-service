package com.legalpro.accountservice.service;

public interface NotificationService {

    /**
     * Send a push notification to a single FCM device token.
     */
    String sendNotification(String token, String title, String body);

    /**
     * Send a push notification with optional custom data payload.
     */
    String sendNotificationWithData(String token, String title, String body, java.util.Map<String, String> data);
}
