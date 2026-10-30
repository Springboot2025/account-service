package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLawyerBillingDto {
    private UUID lawyerUuid;
    private String name;
    private String email;

    // True for a lawyer who joined a firm (not the firm admin) -- their billing
    // is managed by the firm admin's subscription, they have none of their own.
    private boolean managedByFirm;

    private boolean subscribed; // false if there's no subscription row at all

    private UUID subscriptionUuid;
    private String planName;
    private String planDuration;
    private Integer status; // 0 inactive, 1 active, 2 cancelled
    private boolean isTrial;
    private LocalDateTime startDate;
    private LocalDateTime renewsAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private BigDecimal monthlyAmount; // current plan price + seat cost, excl. GST

    // FIRM only
    private boolean isFirm;
    private Integer seatCount;
    private Integer usedSeats;

    private List<AdminLawyerInvoiceDto> invoices; // newest first, every type/status (admin sees everything)
}
