package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUsersSummaryDto {
    private long allUsers;
    private long clients;
    private long lawyers;
    private long firms;
}
