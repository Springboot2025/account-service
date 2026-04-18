package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FirmDashboardSummaryDto {
    private long activeLawyers;
    private long totalCases;
    private long pendingInvites;
    private double performance;
}
