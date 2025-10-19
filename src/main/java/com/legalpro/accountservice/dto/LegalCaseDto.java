package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LegalCaseDto {
    private Long id;
    private UUID uuid;
    private String caseNumber;

    // These fields caused the error — we add them back:
    private String listing;
    private LocalDate courtDate;
    private BigDecimal availableTrustFunds;
    private String followUp;

    private Long statusId;
    private String statusName;
    private UUID clientUuid;
    private UUID lawyerUuid;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
