package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SharedDocumentResponseDto {

    private UUID uuid;

    private UUID documentUuid;
    private UUID lawyerUuid;
    private UUID clientUuid;
    private UUID caseUuid;

    private String remarks;

    private LocalDateTime createdAt;
}
