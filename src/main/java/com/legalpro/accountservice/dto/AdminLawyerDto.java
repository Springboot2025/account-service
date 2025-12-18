package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record AdminLawyerDto(
        UUID lawyerUuid,
        String fullName,
        boolean isActive,
        String specialization,
        long closedMatters,
        long activeMatters,
        long pendingMatters,
        double rating,
        int reviewCount,
        String profilePictureUrl,
        long sinceYears
) {

    private static final String GCS_PUBLIC_BASE =
            "https://storage.googleapis.com";

    public static AdminLawyerDto from(Account a, CaseStatsProjection s) {

        /* ---------- Full name from JSON ---------- */
        String fullName = "";

        if (a.getPersonalDetails() != null) {
            JsonNode pd = a.getPersonalDetails();
            String firstName = pd.hasNonNull("firstName")
                    ? pd.get("firstName").asText()
                    : "";
            String lastName = pd.hasNonNull("lastName")
                    ? pd.get("lastName").asText()
                    : "";
            fullName = (firstName + " " + lastName).trim();
        }

        /* ---------- Case stats (null-safe) ---------- */
        long closed  = s != null && s.getClosed()  != null ? s.getClosed()  : 0;
        long active  = s != null && s.getActive()  != null ? s.getActive()  : 0;
        long pending = s != null && s.getPending() != null ? s.getPending() : 0;

        /* ---------- Since years ---------- */
        long sinceYears = 0;
        if (a.getCreatedAt() != null) {
            sinceYears = ChronoUnit.YEARS.between(
                    a.getCreatedAt(),
                    LocalDateTime.now()
            );
        }

        /* ---------- Profile picture (GCS → public URL) ---------- */
        String profilePic = convertGcsUrl(a.getProfilePictureUrl());

        return new AdminLawyerDto(
                a.getUuid(),
                fullName,
                a.isActive(),
                "Not Specified",   // specialization (intentionally deferred)
                closed,
                active,
                pending,
                0,     // now supported
                0,
                profilePic,
                sinceYears
        );
    }

    private static String convertGcsUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }
}
