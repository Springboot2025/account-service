package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.DocumentCategoryDto;
import com.legalpro.accountservice.entity.DocumentCategory;

public class DocumentCategoryMapper {

    private DocumentCategoryMapper() {
        // utility class
    }

    public static DocumentCategoryDto toDto(DocumentCategory entity) {
        if (entity == null) {
            return null;
        }

        return DocumentCategoryDto.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .displayName(entity.getDisplayName())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
