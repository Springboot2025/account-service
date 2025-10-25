package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.CompanyDto;
import com.legalpro.accountservice.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyDto toDto(Company entity) {
        if (entity == null) return null;

        return CompanyDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .removedAt(entity.getRemovedAt())
                .build();
    }

    public Company toEntity(CompanyDto dto) {
        if (dto == null) return null;

        return Company.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .name(dto.getName())
                .description(dto.getDescription())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .removedAt(dto.getRemovedAt())
                .build();
    }
}
