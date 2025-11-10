package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.DisputeDto;
import com.legalpro.accountservice.entity.Dispute;
import org.springframework.stereotype.Component;

@Component
public class DisputeMapper {

    public DisputeDto toDto(Dispute entity) {
        if (entity == null) return null;

        return DisputeDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .organization(entity.getOrganization())
                .role(entity.getRole())
                .reference(entity.getReference())
                .incidentDate(entity.getIncidentDate())
                .typeOfDispute(entity.getTypeOfDispute())
                .description(entity.getDescription())
                .resolutionRequested(entity.getResolutionRequested())
                .confirmAccuracy(entity.isConfirmAccuracy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Dispute toEntity(DisputeDto dto) {
        if (dto == null) return null;

        return Dispute.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .organization(dto.getOrganization())
                .role(dto.getRole())
                .reference(dto.getReference())
                .incidentDate(dto.getIncidentDate())
                .typeOfDispute(dto.getTypeOfDispute())
                .description(dto.getDescription())
                .resolutionRequested(dto.getResolutionRequested())
                .confirmAccuracy(dto.isConfirmAccuracy())
                .build();
    }
}
