package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.LawyerRatingDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LawyerRating;
import org.springframework.stereotype.Component;

@Component
public class LawyerRatingMapper {

    public LawyerRatingDto toDto(LawyerRating entity) {
        if (entity == null) return null;

        return LawyerRatingDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .clientUuid(entity.getClientUuid())
                .rating(entity.getRating())
                .review(entity.getReview())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

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

    public LawyerRatingDto toDto(LawyerRating entity, Account clientAccount, String profilePicUrl) {
        if (entity == null) return null;

        return LawyerRatingDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .clientUuid(entity.getClientUuid())
                .rating(entity.getRating())
                .review(entity.getReview())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .clientName(clientAccount != null ? extractClientName(clientAccount) : null)
                .clientProfilePictureUrl(profilePicUrl)
                .build();
    }

    private String extractClientName(Account acc) {
        if (acc.getPersonalDetails() == null) return null;

        var pd = acc.getPersonalDetails();
        if (pd.hasNonNull("fullName")) return pd.get("fullName").asText();
        if (pd.hasNonNull("name")) return pd.get("name").asText();

        if (pd.hasNonNull("firstName") && pd.hasNonNull("lastName")) {
            return pd.get("firstName").asText() + " " + pd.get("lastName").asText();
        }
        return null;
    }
}
