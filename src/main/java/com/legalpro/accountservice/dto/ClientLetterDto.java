package com.legalpro.accountservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLetterDto {

    private UUID sharedUuid;
    private CaseInfo caseInfo;
    private DocumentInfo document;
    private LocalDateTime sentDate;
    private String remarks;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaseInfo {
        private UUID uuid;
        private String caseNumber;
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentInfo {
        private UUID uuid;
        private String name;
        private String fileType;
        private String fileUrl;
    }
}
