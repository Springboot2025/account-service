package com.legalpro.accountservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CompanyInviteRequestDto {
    private String email;
    private UUID companyUuid; // the company sending invite
}
