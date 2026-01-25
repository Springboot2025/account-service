package com.legalpro.accountservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LawyerClientsSummaryDto {

    private long totalContacts;
    private double totalContactsGrowth;

    private long activeCases;
    private double activeCasesGrowth;

    private long pendingCases;
    private double pendingCasesGrowth;

    private long pendingReminders;
}
