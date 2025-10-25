package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.NotificationLogDto;
import com.legalpro.accountservice.entity.NotificationLog;
import com.legalpro.accountservice.repository.NotificationLogRepository;
import com.legalpro.accountservice.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogRepository repository;

    @Override
    public void saveLog(NotificationLogDto dto) {
        try {
            NotificationLog entity = NotificationLog.builder()
                    .userUuid(dto.getUserUuid())
                    .deviceId(dto.getDeviceId())
                    .fcmToken(dto.getFcmToken())
                    .messageId(dto.getMessageId())
                    .title(dto.getTitle())
                    .body(dto.getBody())
                    .payload(dto.getPayload())
                    .status(dto.getStatus())
                    .errorMessage(dto.getErrorMessage())
                    .build();

            repository.save(entity);
            log.info("📦 Notification log saved for user {}", dto.getUserUuid());
        } catch (Exception e) {
            log.error("❌ Failed to save notification log: {}", e.getMessage());
        }
    }

    @Override
    public List<NotificationLogDto> getLogsByUser(UUID userUuid) {
        return repository.findByUserUuidOrderBySentAtDesc(userUuid)
                .stream()
                .map(entity -> NotificationLogDto.builder()
                        .id(entity.getId())
                        .userUuid(entity.getUserUuid())
                        .deviceId(entity.getDeviceId())
                        .fcmToken(entity.getFcmToken())
                        .messageId(entity.getMessageId())
                        .title(entity.getTitle())
                        .body(entity.getBody())
                        .payload(entity.getPayload())
                        .status(entity.getStatus())
                        .errorMessage(entity.getErrorMessage())
                        .sentAt(entity.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }
}
