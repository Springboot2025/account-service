package com.legalpro.accountservice.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.legalpro.accountservice.dto.NotificationLogDto;
import com.legalpro.accountservice.service.NotificationLogService;
import com.legalpro.accountservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogService notificationLogService;

    @Override
    public String sendNotification(UUID userUuid, String token, String title, String body) {
        String messageId = null;
        String status;
        String errorMessage = null;

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            messageId = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification sent successfully to user {}: {}", userUuid, messageId);
            status = "SENT";
        } catch (Exception e) {
            log.error("❌ Failed to send notification to user {} (token={}): {}", userUuid, token, e.getMessage());
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        // Save notification log
        try {
            NotificationLogDto logDto = NotificationLogDto.builder()
                    .userUuid(userUuid)
                    .fcmToken(token)
                    .messageId(messageId)
                    .title(title)
                    .body(body)
                    .payload(null)
                    .status(status)
                    .errorMessage(errorMessage)
                    .sentAt(ZonedDateTime.now())
                    .build();

            notificationLogService.saveLog(logDto);
        } catch (Exception ex) {
            log.error("⚠️ Failed to persist notification log for user {}: {}", userUuid, ex.getMessage());
        }

        return status.equals("SENT")
                ? "✅ Sent successfully: " + messageId
                : "❌ Failed for token: " + token + " → " + errorMessage;
    }

    @Override
    public String sendNotificationWithData(UUID userUuid, String token, String title, String body, Map<String, String> data) {
        String messageId = null;
        String status;
        String errorMessage = null;

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            messageId = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Notification with data sent successfully to user {}: {}", userUuid, messageId);
            status = "SENT";
        } catch (Exception e) {
            log.error("❌ Failed to send notification with data to user {} (token={}): {}", userUuid, token, e.getMessage());
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        // Save notification log
        try {
            NotificationLogDto logDto = NotificationLogDto.builder()
                    .userUuid(userUuid)
                    .fcmToken(token)
                    .messageId(messageId)
                    .title(title)
                    .body(body)
                    .payload(data != null ? data.toString() : null)
                    .status(status)
                    .errorMessage(errorMessage)
                    .sentAt(ZonedDateTime.now())
                    .build();

            notificationLogService.saveLog(logDto);
        } catch (Exception ex) {
            log.error("⚠️ Failed to persist notification log for user {}: {}", userUuid, ex.getMessage());
        }

        return status.equals("SENT")
                ? "✅ Sent successfully: " + messageId
                : "❌ Failed for token: " + token + " → " + errorMessage;
    }
}
