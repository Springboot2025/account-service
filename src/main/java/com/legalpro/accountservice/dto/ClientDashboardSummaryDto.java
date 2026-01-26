package com.legalpro.accountservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDashboardSummaryDto {

    private long activeCases;
    private long newCasesThisMonth;
    private long upcomingDates;

    // Placeholders
    private Object trustBalance;
    private Object hoursBilled;
}
