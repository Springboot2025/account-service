package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LawyerRating;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LawyerRatingDto {

    private Long id;
    private UUID uuid;

    private UUID lawyerUuid;
    private UUID clientUuid;

    private BigDecimal rating;
    private String review;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* -------- NEW FIELDS -------- */
    private String clientName;
    private String clientProfilePictureUrl;


    /* ============================================================
       STATIC FACTORY: SAME STYLE AS PublicLawyerProfileDto.from()
       ============================================================ */
    public static LawyerRatingDto from(LawyerRating entity, Account clientAccount) {

        String clientName = null;
        String clientPic = null;

        if (clientAccount != null) {
            clientName = extractNameFromAccount(clientAccount);
            clientPic = convertGcsUrl(clientAccount.getProfilePictureUrl());
        }

        return LawyerRatingDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .clientUuid(entity.getClientUuid())
                .rating(entity.getRating())
                .review(entity.getReview())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .clientName(clientName)
                .clientProfilePictureUrl(clientPic)
                .build();
    }


    /* -------- Helper: Extract name from Account -------- */
    private static String extractNameFromAccount(Account acc) {

        if (acc == null || acc.getPersonalDetails() == null) return null;

        var pd = acc.getPersonalDetails();

        if (pd.hasNonNull("fullName")) return pd.get("fullName").asText();
        if (pd.hasNonNull("name")) return pd.get("name").asText();

        if (pd.hasNonNull("firstName") && pd.hasNonNull("lastName")) {
            return pd.get("firstName").asText() + " " + pd.get("lastName").asText();
        }

        return null;
    }

    /* -------- Helper: Convert GCS URL -------- */
    private static String convertGcsUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("gs://")) {
            return "https://storage.googleapis.com/" +
                    fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }
}