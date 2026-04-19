package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FirmDashboardSummaryDto {
    private long totalLawyers;
    private long activeCases;
    private long totalClients;
    private double performance;
}
