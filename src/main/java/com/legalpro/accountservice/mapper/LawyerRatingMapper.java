package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LawyerRatingDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LawyerRating;
import org.springframework.stereotype.Component;

@Component
public class LawyerRatingMapper {

    /**
     * Convert entity → DTO including enriched client fields.
     * DTO handles name + profile pic extraction.
     */
    public LawyerRatingDto toDto(LawyerRating entity, Account clientAccount) {
        if (entity == null) return null;
        return LawyerRatingDto.from(entity, clientAccount);
    }

    /**
     * Convert DTO → entity (no changes).
     * Client name / picture are NOT persisted.
     */
    public LawyerRating toEntity(LawyerRatingDto dto) {
        if (dto == null) return null;

        return LawyerRating.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .clientUuid(dto.getClientUuid())
                .rating(dto.getRating())
                .review(dto.getReview())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}