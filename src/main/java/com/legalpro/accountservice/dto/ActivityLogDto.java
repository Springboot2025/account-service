package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ActivityLogDto {

    private UUID uuid;
    private LocalDateTime timestamp;

    private String activityType;
    private String description;

    private String actorName;

    private UUID caseUuid;
    private UUID clientUuid;
    private UUID lawyerUuid;
    private UUID referenceUuid;

    private String actorProfilePictureUrl;
    private String clientProfilePictureUrl;
    private String lawyerProfilePictureUrl;

    private Object metadata;
}
