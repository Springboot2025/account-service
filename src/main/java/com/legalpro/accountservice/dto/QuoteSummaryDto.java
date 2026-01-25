package com.legalpro.accountservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuoteSummaryDto {

    private long newRequests;
    private long urgent;
    private long acceptedToday;
    private long thisMonth;
}
