package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminCaseDto {
    private UUID caseUuid;
    private String caseNumber;

    private String title;
    private String caseType;

    private String clientName;
    private String clientProfilePictureUrl;

    private String lawyerName;
    private String lawyerProfilePictureUrl;

    private String status;
    private LocalDateTime createdAt;
}