package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactSummaryDto {
    private UUID clientUuid;
    private String contactName;
    private String caseNumber;
    private String profilePictureUrl;
    private String caseStatus;
    private String contactInfo; // extracted from Account.contactInformation JSON
    private Instant lastContactDate; // from Message
    private String reminder; // from LegalCase.followUp
    private LegalCaseDto legalCase;
}
