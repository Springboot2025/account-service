package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDeclinedInvoiceDto {
    private UUID subscriptionUuid;
    private String name;
    private String email;
    private String invoiceNumber;
    private String planName;
    private BigDecimal totalAmount;
    private int failedAttempts;
    private LocalDateTime lastAttemptAt;
}
