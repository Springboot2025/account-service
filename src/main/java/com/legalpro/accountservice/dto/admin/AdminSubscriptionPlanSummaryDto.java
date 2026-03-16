package com.legalpro.accountservice.dto.admin;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSubscriptionPlanSummaryDto {

    private String planName;

    private long subscribers;

    private double monthlyRevenue;
}