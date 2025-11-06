package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LawyerInvoiceDto {
    private Long id;
    private UUID uuid;

    private UUID caseUuid;
    private UUID lawyerUuid;

    private BigDecimal trustBalance;
    private BigDecimal amountRequested;
    private LocalDate dueDate;
    private String lastActivity;
    private String status;

    private String stripeSessionId;
    private String stripePaymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
