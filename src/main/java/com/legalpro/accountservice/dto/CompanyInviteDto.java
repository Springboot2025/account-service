package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CompanyInviteDto {
    private String email;
    private UUID companyUuid;
    private boolean expired;
    private boolean used;
}
