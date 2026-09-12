package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummaryDto {

    private long totalTickets;
    private long urgentCount;
    private long inProgressCount;
    private long resolvedCount;
}
