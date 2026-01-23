package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.entity.ActivityLog;
import com.legalpro.accountservice.repository.ActivityLogRepository;
import com.legalpro.accountservice.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void logActivity(
            String activityType,
            String description,
            UUID actorUuid,
            String actorName,
            UUID lawyerUuid,
            UUID clientUuid,
            UUID caseUuid,
            UUID referenceUuid,
            Object metadata
    ) {
        ActivityLog log = ActivityLog.builder()
                .activityType(activityType)
                .description(description)
                .actorUuid(actorUuid)
                .actorName(actorName)
                .lawyerUuid(lawyerUuid)
                .clientUuid(clientUuid)
                .caseUuid(caseUuid)
                .referenceUuid(referenceUuid)
                .timestamp(LocalDateTime.now())
                .metadata(metadata != null ? serialize(metadata) : null)
                .build();

        activityLogRepository.save(log);
    }

    private String serialize(Object metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return null; // never break business flow
        }
    }
}
