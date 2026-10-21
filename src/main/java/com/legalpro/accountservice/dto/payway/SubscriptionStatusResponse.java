package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The logged-in payer's current subscription, read fresh from the DB --
 * unlike billing history, this doesn't need to be inferred from invoices.
 */
@Data
@Builder
public class SubscriptionStatusResponse {
    private boolean subscribed;        // false when there is no row at all
    private UUID subscriptionUuid;
    private Long planId;
    private String planName;
    private String planDuration;       // monthly | yearly
    private Integer status;            // 0 inactive, 1 active, 2 cancelled
    private LocalDateTime startDate;
    private LocalDateTime renewsAt;
    private LocalDateTime cancelledAt;
    private BigDecimal monthlyAmount;  // plan price + seats, excl. GST, at the plan's current duration price
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;

    // FIRM only
    private boolean isFirm;
    private Integer additionalSeats;   // purchased, beyond the admin's own
    private Integer usedSeats;         // lawyers actually invited/joined
}
