package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayWayPaymentResponse {
    private UUID uuid;
    private String orderNumber;
    private String invoiceNumber;
    private String invoiceStatus;       // DUE | PAID
    private LocalDate periodStart;
    private LocalDate periodEnd;        // next billing date
    private UUID subscriptionUuid;      // set once approved
    private Integer subscriptionStatus; // 1 = active
    private LocalDateTime renewsAt;
    private String status;              // APPROVED | DECLINED | ERROR | PENDING
    private Long planId;
    private String planDuration;
    private String customerNumber;
    private BigDecimal amount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private Long paywayTransactionId;
    private String receiptNumber;
    private String responseCode;
    private String responseText;
    private String cardScheme;
    private String maskedCardNumber;
    private LocalDate settlementDate;
    private String errorMessage;
    private LocalDateTime createdAt;

    // Trial signup only -- amount/gstAmount/totalAmount above are what was actually
    // charged today (the small, refunded verification amount); these describe what
    // happens once the trial ends.
    private Boolean trial;
    private LocalDateTime trialEndsAt;
    private Boolean verificationRefunded;
    private BigDecimal renewalAmount;
    private BigDecimal renewalGstAmount;
    private BigDecimal renewalTotalAmount;
}
