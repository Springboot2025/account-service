package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class InvitationSummaryDto {

    private long totalSent;
    private long pending;
    private long accepted;
    private long declined; // placeholder
    private long expired;
}
