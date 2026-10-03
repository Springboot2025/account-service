package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// One row of the Billing History screen (one invoice)
@Data
@Builder
public class BillingHistoryItemDto {
    private UUID invoiceUuid;
    private String invoiceNumber;
    private String invoiceType;         // INITIAL | RENEWAL
    private LocalDate date;             // billing date = periodStart
    private String description;         // e.g. "Subscription for Individual Lawyers — Monthly"
    private Long planId;
    private String planName;
    private String planDuration;        // monthly | yearly
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;          // excl. GST
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;              // PAID | OVERDUE | DUE
    private LocalDateTime paidAt;
    // From the approved payment (null unless PAID)
    private String orderNumber;
    private String receiptNumber;
    private String cardScheme;
    private String maskedCardNumber;
}
