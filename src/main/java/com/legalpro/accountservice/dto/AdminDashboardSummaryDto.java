package com.legalpro.accountservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardSummaryDto {

    private long totalUsers;
    private long activeCases;

    private double monthlyRevenue;      // placeholder
    private long activeSubscriptions;   // placeholder
}