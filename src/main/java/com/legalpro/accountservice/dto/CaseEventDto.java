package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CaseEventDto {

    private Long id;
    private UUID uuid;
    private UUID caseUuid;
    private Long caseId;
    private LocalDate date;
    private String type;
    private String title;
    private String details;
    private String status;
    private LocalDate relatedDate;

    private String userName;
    private LocalDateTime createdAt;
}
