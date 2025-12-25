package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {

    private UUID userUuid;
    private String email;
    private boolean active;

    // Latest case (nullable)
    private UUID latestCaseUuid;
    private String latestCaseNumber;
    private String latestCaseCategory;
    private String latestCaseStatus;
    private LocalDateTime latestCaseCreatedAt;
}
