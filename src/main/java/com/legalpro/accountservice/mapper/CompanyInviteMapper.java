package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.CompanyInviteDto;
import com.legalpro.accountservice.entity.CompanyInvite;

public class CompanyInviteMapper {

    public static CompanyInviteDto toDto(CompanyInvite invite) {
        if (invite == null) return null;

        boolean expired = invite.getExpiresAt().isBefore(java.time.LocalDateTime.now());

        return CompanyInviteDto.builder()
                .email(invite.getEmail())
                .companyUuid(invite.getCompanyUuid())
                .expired(expired)
                .used(invite.isUsed())
                .build();
    }
}
