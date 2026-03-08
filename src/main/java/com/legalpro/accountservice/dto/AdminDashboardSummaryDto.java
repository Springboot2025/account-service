package com.legalpro.accountservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardSummaryDto {

    private long totalUsers;
    private double usersChangePercent;
    private long activeCases;
    private double activeCasesChangePercent;

    private double monthlyRevenue;      // placeholder
    private double revenueChangePercent;
    private long activeSubscriptions;   // placeholder
    private double subscriptionsChangePercent;
}