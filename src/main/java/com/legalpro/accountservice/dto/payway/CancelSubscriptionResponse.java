package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CancelSubscriptionResponse {
    private UUID subscriptionUuid;
    private Integer status;             // 2 = cancelled
    private Long planId;
    private String planName;
    private String planDuration;
    private LocalDateTime startDate;    // member since
    private LocalDateTime accessUntil;  // = renewsAt: access continues until then, no further charges
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Integer voidedInvoices;     // open renewal invoices (DUE / OVERDUE) voided by the cancel
    private Boolean paywayPaymentsStopped; // PayWay customer locked against charges (false if that call failed)
}
