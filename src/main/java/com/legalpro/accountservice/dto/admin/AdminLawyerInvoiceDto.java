package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLawyerInvoiceDto {
    private String invoiceNumber;
    private String invoiceType;  // INITIAL | RENEWAL | SEAT_ADDITION | VERIFICATION
    private String status;       // DUE | PAID | OVERDUE | VOID | REFUNDED
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private int seatCount;
    private LocalDateTime paidAt;
    private int failedAttempts;
}
