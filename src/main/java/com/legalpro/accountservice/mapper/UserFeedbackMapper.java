package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.UserFeedbackDto;
import com.legalpro.accountservice.entity.UserFeedback;
import org.springframework.stereotype.Component;

@Component
public class UserFeedbackMapper {

    public UserFeedbackDto toDto(UserFeedback entity) {
        if (entity == null) return null;

        return UserFeedbackDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .userUuid(entity.getUserUuid())
                .rating(entity.getRating())
                .review(entity.getReview())
                .isPublic(entity.getIsPublic())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserFeedback toEntity(UserFeedbackDto dto) {
        if (dto == null) return null;

        return UserFeedback.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .userUuid(dto.getUserUuid())
                .rating(dto.getRating())
                .review(dto.getReview())
                .isPublic(dto.getIsPublic())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
