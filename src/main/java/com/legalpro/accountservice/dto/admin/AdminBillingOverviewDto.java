package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBillingOverviewDto {
    private double monthlyRevenue;        // actual cash collected this calendar month
    private double revenueChangePercent;  // vs last calendar month
    private double mrr;                   // current run-rate (excludes trials)
    private long activeSubscriptions;
    private long activeTrials;
    private List<AdminUpcomingChargeDto> upcomingCharges;   // next 7 days
    private List<AdminDeclinedInvoiceDto> recentDeclines;   // most recent OVERDUE invoices
}
