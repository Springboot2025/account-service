package com.legalpro.accountservice.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentShareRecipientDto {

    private UUID clientUuid;
    private String contactName;
    private String contactInfo;

    private List<CaseDto> cases;

    // ----------------------------
    // Nested DTO for case details
    // ----------------------------
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaseDto {

        private Long id;
        private UUID uuid;
        private String caseNumber;
        private String listing;
        private String name;
        private String caseTypeName;
    }
}
