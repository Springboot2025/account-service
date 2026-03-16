package com.legalpro.accountservice.dto.admin;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSubscriptionsSummaryDto {
    private long totalSubscribers;
    private double mrr;
    private long newThisMonth;
    private long cancelled;
    private double retentionRate;
}