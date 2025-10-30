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

        log.info("🚀 Sending notification: userUuid={}, title={}, tokenPrefix={}",
                userUuid, title, token != null ? token.substring(0, Math.min(10, token.length())) : "null");

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            messageId = FirebaseMessaging.getInstance().send(message);
            status = "SENT";
            log.info("✅ Notification sent successfully: userUuid={}, messageId={}", userUuid, messageId);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("❌ Failed to send notification: userUuid={}, token={}, error={}", userUuid, token, e.getMessage(), e);
        }

        // --- Save log in DB ---
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

            log.info("🟡 Attempting to save notification log: userUuid={}, status={}", userUuid, status);
            notificationLogService.saveLog(logDto);
            log.info("📦 Notification log saved successfully: userUuid={}, status={}", userUuid, status);
        } catch (Exception ex) {
            log.error("🔥 Failed to persist notification log in DB: userUuid={}, error={}", userUuid, ex.getMessage(), ex);
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

        log.info("🚀 Sending data notification: userUuid={}, title={}, tokenPrefix={}, dataKeys={}",
                userUuid, title, token != null ? token.substring(0, Math.min(10, token.length())) : "null",
                data != null ? data.keySet() : "none");

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
            status = "SENT";
            log.info("✅ Notification with data sent: userUuid={}, messageId={}", userUuid, messageId);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("❌ Failed to send notification with data: userUuid={}, token={}, error={}", userUuid, token, e.getMessage(), e);
        }

        // --- Save log in DB ---
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

            log.info("🟡 Attempting to save notification log with data: userUuid={}, status={}", userUuid, status);
            notificationLogService.saveLog(logDto);
            log.info("📦 Notification log (with data) saved successfully: userUuid={}, status={}", userUuid, status);
        } catch (Exception ex) {
            log.error("🔥 Failed to persist notification log (with data): userUuid={}, error={}", userUuid, ex.getMessage(), ex);
        }

        return status.equals("SENT")
                ? "✅ Sent successfully: " + messageId
                : "❌ Failed for token: " + token + " → " + errorMessage;
    }
}
