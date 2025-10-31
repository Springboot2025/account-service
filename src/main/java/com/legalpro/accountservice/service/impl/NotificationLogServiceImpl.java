package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.NotificationLogDto;
import com.legalpro.accountservice.entity.NotificationLog;
import com.legalpro.accountservice.repository.NotificationLogRepository;
import com.legalpro.accountservice.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveLog(NotificationLogDto dto) {
        try {
            NotificationLog.NotificationLogBuilder entityBuilder = NotificationLog.builder()
                    .userUuid(dto.getUserUuid())
                    .deviceId(dto.getDeviceId())
                    .fcmToken(dto.getFcmToken())
                    .messageId(dto.getMessageId())
                    .title(dto.getTitle())
                    .body(dto.getBody())
                    .status(dto.getStatus())
                    .errorMessage(dto.getErrorMessage());

            // ✅ convert Map → JsonNode (for jsonb)
            if (dto.getPayload() != null) {
                entityBuilder.payload(objectMapper.convertValue(dto.getPayload(), JsonNode.class));
            }

            NotificationLog entity = entityBuilder.build();
            repository.save(entity);

            log.info("📦 Notification log saved for user {}", dto.getUserUuid());
        } catch (Exception e) {
            log.error("❌ Failed to save notification log", e);
        }
    }


    @Override
    public List<NotificationLogDto> getLogsByUser(UUID userUuid) {
        return repository.findByUserUuidOrderBySentAtDesc(userUuid)
                .stream()
                .map(entity -> {
                    Map<String, Object> payloadMap = null;
                    if (entity.getPayload() != null) {
                        payloadMap = objectMapper.convertValue(entity.getPayload(), Map.class);
                    }

                    return NotificationLogDto.builder()
                            .id(entity.getId())
                            .userUuid(entity.getUserUuid())
                            .deviceId(entity.getDeviceId())
                            .fcmToken(entity.getFcmToken())
                            .messageId(entity.getMessageId())
                            .title(entity.getTitle())
                            .body(entity.getBody())
                            .payload(payloadMap)  // ✅ Convert JsonNode → Map<String, Object>
                            .status(entity.getStatus())
                            .errorMessage(entity.getErrorMessage())
                            .sentAt(entity.getSentAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

}