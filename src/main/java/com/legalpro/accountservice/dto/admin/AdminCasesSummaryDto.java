package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCasesSummaryDto {

    private long totalCases;
    private long active;
    private long pending;
    private long newCases;
    private long closed;

}