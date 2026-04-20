package com.legalpro.accountservice.dto.admin;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FirmRecentCaseDto {
    private UUID caseUuid;
    private String caseNumber;
    private String caseTitle;

    private String clientName;

    private String status;
    private LocalDate courtDate;

    private String lawyerName;
    private String profilePictureUrl;
}
