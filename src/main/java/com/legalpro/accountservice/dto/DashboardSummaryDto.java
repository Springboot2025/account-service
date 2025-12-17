package com.legalpro.accountservice.dto;

public class DashboardSummaryDto {

    private final long totalLawyers;
    private final long activeLawyers;
    private final long totalFirms;
    private final long activeFirms;

    public DashboardSummaryDto(
            long totalLawyers,
            long activeLawyers,
            long totalFirms,
            long activeFirms
    ) {
        this.totalLawyers = totalLawyers;
        this.activeLawyers = activeLawyers;
        this.totalFirms = totalFirms;
        this.activeFirms = activeFirms;
    }

    public long getTotalLawyers() {
        return totalLawyers;
    }

    public long getActiveLawyers() {
        return activeLawyers;
    }

    public long getTotalFirms() {
        return totalFirms;
    }

    public long getActiveFirms() {
        return activeFirms;
    }
}
