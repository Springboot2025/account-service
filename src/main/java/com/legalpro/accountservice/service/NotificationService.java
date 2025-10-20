package com.legalpro.accountservice.service;

public interface NotificationService {

    /**
     * Send a push notification to a single FCM device token.
     */
    void sendNotification(String token, String title, String body);

    /**
     * Send a push notification with optional custom data payload.
     */
    void sendNotificationWithData(String token, String title, String body, java.util.Map<String, String> data);
}
