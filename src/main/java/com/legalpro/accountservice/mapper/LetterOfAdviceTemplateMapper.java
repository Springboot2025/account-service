package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LetterOfAdviceTemplateDto;
import com.legalpro.accountservice.entity.LetterOfAdviceTemplate;
import org.springframework.stereotype.Component;

@Component
public class LetterOfAdviceTemplateMapper {

    public LetterOfAdviceTemplateDto toDto(LetterOfAdviceTemplate entity) {
        if (entity == null) return null;

        return LetterOfAdviceTemplateDto.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
