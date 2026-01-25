package com.legalpro.accountservice.dto;

import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveCasesSummaryDto {
    private long totalCases;
    private long urgent;
    private long hearingsThisWeek;
    private long wonThisMonth;
}

