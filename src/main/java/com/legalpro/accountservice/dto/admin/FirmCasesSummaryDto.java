package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FirmCasesSummaryDto {
    private long total;
    private long unassigned;
    private long inProgress;
    private long completed;
}
