package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LawyerDocumentSubheadingDto;
import com.legalpro.accountservice.entity.LawyerDocumentSubheading;

public class LawyerDocumentSubheadingMapper {

    private LawyerDocumentSubheadingMapper() {
        // utility class
    }

    public static LawyerDocumentSubheadingDto toDto(LawyerDocumentSubheading entity) {
        if (entity == null) {
            return null;
        }

        return LawyerDocumentSubheadingDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .categoryId(entity.getCategory().getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
