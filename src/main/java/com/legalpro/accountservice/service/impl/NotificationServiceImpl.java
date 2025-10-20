package com.legalpro.accountservice.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.legalpro.accountservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendNotification(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification sent successfully: {}", response);
        } catch (Exception e) {
            log.error("❌ Failed to send notification to token {}: {}", token, e.getMessage());
        }
    }

    @Override
    public void sendNotificationWithData(String token, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification with data sent successfully: {}", response);
        } catch (Exception e) {
            log.error("❌ Failed to send notification with data: {}", e.getMessage());
        }
    }
}
