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
public class AdminUpcomingChargeDto {
    private UUID subscriptionUuid;
    private String name;
    private String email;
    private String planName;
    private boolean trialConversion; // true = this is a trial's first real charge
    private BigDecimal amountDue;
    private LocalDateTime chargeDate;
}
