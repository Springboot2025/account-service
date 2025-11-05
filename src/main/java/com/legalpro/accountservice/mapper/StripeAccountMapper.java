package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.StripeAccountDto;
import com.legalpro.accountservice.entity.StripeAccount;
import org.springframework.stereotype.Component;

@Component
public class StripeAccountMapper {

    public StripeAccountDto toDto(StripeAccount entity) {
        if (entity == null) return null;

        return StripeAccountDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .stripeAccountId(entity.getStripeAccountId())
                .chargesEnabled(entity.isChargesEnabled())
                .payoutsEnabled(entity.isPayoutsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public StripeAccount toEntity(StripeAccountDto dto) {
        if (dto == null) return null;

        return StripeAccount.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .stripeAccountId(dto.getStripeAccountId())
                .chargesEnabled(dto.isChargesEnabled())
                .payoutsEnabled(dto.isPayoutsEnabled())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
