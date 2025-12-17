package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;

import java.time.LocalDateTime;
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
        int reviewCount
) {
    public static AdminLawyerDto from(Account a, CaseStatsProjection s) {

        String fullName = "";

        if (a.getPersonalDetails() != null) {
            JsonNode pd = a.getPersonalDetails();
            String firstName = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
            String lastName  = pd.hasNonNull("lastName")  ? pd.get("lastName").asText()  : "";
            fullName = (firstName + " " + lastName).trim();
        }

        long closed  = s != null && s.getClosed()  != null ? s.getClosed()  : 0;
        long active  = s != null && s.getActive()  != null ? s.getActive()  : 0;
        long pending = s != null && s.getPending() != null ? s.getPending() : 0;

        return new AdminLawyerDto(
                a.getUuid(),
                fullName,
                a.isActive(),
                "Not Specified",   // specialization (safe default)
                closed,
                active,
                pending,
                0.0,               // rating (ignored for now)
                0                  // reviewCount (ignored for now)
        );
    }

}


