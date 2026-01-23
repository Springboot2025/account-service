package com.legalpro.accountservice.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.ActivityLogDto;
import com.legalpro.accountservice.entity.ActivityLog;

public class ActivityLogMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ActivityLogDto toDto(ActivityLog log) {

        return ActivityLogDto.builder()
                .uuid(log.getUuid())
                .timestamp(log.getTimestamp())
                .activityType(log.getActivityType())
                .description(log.getDescription())
                .actorName(log.getActorName())
                .caseUuid(log.getCaseUuid())
                .clientUuid(log.getClientUuid())
                .lawyerUuid(log.getLawyerUuid())
                .referenceUuid(log.getReferenceUuid())
                .metadata(
                        log.getMetadata() != null
                                ? toJson(log.getMetadata())
                                : null
                )
                .build();
    }

    private static Object toJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
