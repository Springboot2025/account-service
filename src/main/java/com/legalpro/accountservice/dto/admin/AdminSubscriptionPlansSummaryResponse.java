package com.legalpro.accountservice.dto.admin;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSubscriptionPlansSummaryResponse {
    private AdminSubscriptionPlanSummaryDto individualLawyers;
    private AdminSubscriptionPlanSummaryDto firms;
}