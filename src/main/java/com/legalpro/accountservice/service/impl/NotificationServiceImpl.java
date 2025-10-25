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

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogService notificationLogService;

    @Override
    public String sendNotification(String token, String title, String body) {
        String responseMsg;
        String messageId = null;
        String status;
        String errorMsg = null;

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            messageId = FirebaseMessaging.getInstance().send(message);
            responseMsg = "✅ Sent successfully: " + messageId;
            status = "SENT";
            log.info(responseMsg);
        } catch (Exception e) {
            responseMsg = "❌ Failed for token: " + token + " → " + e.getMessage();
            errorMsg = e.getMessage();
            status = "FAILED";
            log.error("❌ Failed to send notification: {}", e.getMessage());
        }

        // Save log (even if failed)
        try {
            notificationLogService.saveLog(NotificationLogDto.builder()
                    .userUuid(extractUserUuid(token))  // optional; see below
                    .fcmToken(token)
                    .messageId(messageId)
                    .title(title)
                    .body(body)
                    .status(status)
                    .errorMessage(errorMsg)
                    .build());
        } catch (Exception ex) {
            log.error("⚠️ Failed to log notification: {}", ex.getMessage());
        }

        return responseMsg;
    }

    @Override
    public String sendNotificationWithData(String token, String title, String body, Map<String, String> data) {
        String responseMsg;
        String messageId = null;
        String status;
        String errorMsg = null;

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
            responseMsg = "✅ Sent successfully: " + messageId;
            status = "SENT";
            log.info(responseMsg);
        } catch (Exception e) {
            responseMsg = "❌ Failed for token: " + token + " → " + e.getMessage();
            errorMsg = e.getMessage();
            status = "FAILED";
            log.error("❌ Failed to send notification with data: {}", e.getMessage());
        }

        // Save log
        try {
            notificationLogService.saveLog(NotificationLogDto.builder()
                    .userUuid(extractUserUuid(token)) // optional if you track via DeviceToken
                    .fcmToken(token)
                    .messageId(messageId)
                    .title(title)
                    .body(body)
                    .payload(data != null ? data.toString() : null)
                    .status(status)
                    .errorMessage(errorMsg)
                    .build());
        } catch (Exception ex) {
            log.error("⚠️ Failed to log notification with data: {}", ex.getMessage());
        }

        return responseMsg;
    }

    /**
     * Optional: Extract or map user UUID if your DeviceToken table maps token → user.
     * For now returns null so it won’t break anything.
     */
    private UUID extractUserUuid(String token) {
        // TODO: Integrate with DeviceTokenService if needed.
        return null;
    }
}
