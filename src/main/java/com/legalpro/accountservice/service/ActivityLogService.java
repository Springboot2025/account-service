package com.legalpro.accountservice.service;

import java.util.UUID;

public interface ActivityLogService {

    void logActivity(
            String activityType,
            String description,
            UUID actorUuid,
            String actorName,
            UUID lawyerUuid,
            UUID clientUuid,
            UUID caseUuid,
            UUID referenceUuid,
            Object metadata
    );
}
