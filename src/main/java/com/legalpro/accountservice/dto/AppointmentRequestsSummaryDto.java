package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentRequestsSummaryDto {
    private long pending;       // PENDING
    private long today;         // today's appointments
    private long upcoming;      // next 7 days
    private long thisMonth;     // appointments this month
}
