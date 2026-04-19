package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FirmClientsSummaryDto {
    private long totalClients;
    private long activeCases;
    private long newThisMonth;
    private double totalRevenue;
}
