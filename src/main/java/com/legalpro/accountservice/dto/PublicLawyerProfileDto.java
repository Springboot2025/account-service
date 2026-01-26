package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record PublicLawyerProfileDto(

        UUID lawyerUuid,
        String fullName,
        String profilePictureUrl,

        boolean isAvailableNow,

        long sinceYears,
        String experienceLabel,

        String specialization,
        JsonNode professionalDetails,

        JsonNode contactInformation,
        JsonNode addressDetails,

        JsonNode educationQualification,
        JsonNode awardsAppreciations,
        JsonNode consultationRates,
        JsonNode languages,
        BigDecimal averageRating,
        int reviewCount

) {

    private static final String GCS_PUBLIC_BASE =
            "https://storage.googleapis.com";

    public static PublicLawyerProfileDto from(
            Account account,
            BigDecimal averageRating,
            int reviewCount
    ) {

        /* ---------- Full name ---------- */
        String fullName = "";
        if (account.getPersonalDetails() != null) {
            JsonNode pd = account.getPersonalDetails();
            String firstName = pd.hasNonNull("firstName")
                    ? pd.get("firstName").asText()
                    : "";
            String lastName = pd.hasNonNull("lastName")
                    ? pd.get("lastName").asText()
                    : "";
            fullName = (firstName + " " + lastName).trim();
        }

        /* ---------- Experience ---------- */
        long sinceYears = 0;
        if (account.getCreatedAt() != null) {
            sinceYears = ChronoUnit.YEARS.between(
                    account.getCreatedAt(),
                    LocalDateTime.now()
            );
        }

        String experienceLabel =
                sinceYears > 0
                        ? sinceYears + "+ years experience"
                        : "Less than 1 year experience";

        /* ---------- Specialization ---------- */
        String specialization = "Not specified";
        if (account.getProfessionalDetails() != null) {
            JsonNode pd = account.getProfessionalDetails();
            if (pd.hasNonNull("specialization")) {
                specialization = pd.get("specialization").asText();
            }
        }

        return new PublicLawyerProfileDto(
                account.getUuid(),
                fullName,
                convertGcsUrl(account.getProfilePictureUrl()),
                false, // availability placeholder
                sinceYears,
                experienceLabel,
                specialization,
                account.getProfessionalDetails(),
                account.getContactInformation(),
                account.getAddressDetails(),
                account.getEducationQualification(),
                account.getAwardsAppreciations(),
                account.getConsultationRates(),
                account.getLanguages(),
                averageRating,
                reviewCount
        );
    }

    private static String convertGcsUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }
}
